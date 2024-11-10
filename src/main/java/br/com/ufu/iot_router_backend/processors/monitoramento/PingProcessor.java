package br.com.ufu.iot_router_backend.processors.monitoramento;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import br.com.ufu.iot_router_backend.config.BandwidthConfig;
import br.com.ufu.iot_router_backend.config.MonitorConfig;
import br.com.ufu.iot_router_backend.config.RyuConfig;
import br.com.ufu.iot_router_backend.enums.QoSAdjustmentTypeEnum;
import br.com.ufu.iot_router_backend.model.Device;
import br.com.ufu.iot_router_backend.observabilidade.LatencyMetricsService;
import br.com.ufu.iot_router_backend.service.GetIpsService;
import br.com.ufu.iot_router_backend.service.MqttClientService;
import br.com.ufu.iot_router_backend.service.QoSService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PingProcessor implements Processor {

    @Autowired
    private BandwidthConfig bandwidthConfig;

    @Autowired
    private GetIpsService getIpsService;

    @Autowired
    private MonitorConfig monitorConfig;

    @Autowired
    private RyuConfig ryuConfig;

    @Autowired
    private QoSService qosService;

    @Autowired
    private MqttClientService mqttClient;

    @Autowired
    private LatencyMetricsService latencyMetricsService;

    @Override
    public void process(Exchange exchange) throws Exception {
        log.info("Monitoramento containers Devices Simulados");
        List<Device> containers = getIpsService.updateDeviceList(
                exchange.getIn().getBody(String.class));
//        log.info(new Gson().toJson(containers));
        execute(containers);
    }

    private void execute(List<Device> containers) throws Exception {
        log.info("execute...");
    	for (Device device : containers) {
    		log.info("Ip {} Name: {}", device.getIp(), device.getName());
            var deviceIp = device.getIp();
            double latency = getLatency(deviceIp);
            System.out.println("Latência do dispositivo " + deviceIp + ": " + latency + " ms");

            latencyMetricsService.recordLatency(latency, device.getName());

            if (latency > monitorConfig.getLatencyThreshold()) {
                log.info("Latência elevada detectada para o dispositivo {} com IP {}.", device.getName(), deviceIp);

                latencyMetricsService.incrementHighLatencyCount(device.getName());

                reduceDataFrequency(device);
                qosService.applyQoS(device, QoSAdjustmentTypeEnum.LATENCY);
                handleNetworkLoad(device, latency);

                latency = getLatency(deviceIp);

                if (latency > monitorConfig.getLatencyThreshold()) {
                    prioritizeCriticalTraffic(device);
                }
            } else {
                revertToPrimaryPathIfNecessary(device);
                log.info("Latência dentro dos limites aceitáveis.");
            }
        }
    }

    private void revertToPrimaryPathIfNecessary(Device device) throws Exception {
        String switchId = getSwitchIdForDevice(device.getIp(), qosService.fetchHostTopology());
        int primaryPortNo = getPrimaryPortNo(switchId, device.getIp());

        if (primaryPortNo != -1) {
            log.info("Retornando tráfego do dispositivo {} para a rota primária", device.getName());
            redirectTraffic(device, switchId, primaryPortNo);
            latencyMetricsService.incrementFailbackEvents(device.getName());
        }
    }


    private void handleNetworkLoad(Device device, double latency) throws Exception {
        String currentSwitchId = getSwitchIdForDevice(device.getIp(), qosService.fetchHostTopology());
        int alternativePortNo = getAlternativePortNo(currentSwitchId);

        if (alternativePortNo != -1) {
            log.info("Redirecionando tráfego do dispositivo {} para uma rota alternativa", device.getName());
            redirectTraffic(device, currentSwitchId, alternativePortNo);
            latencyMetricsService.incrementRedirectEvents(device.getName());
        } else {
            log.warn("Nenhuma rota alternativa disponível para o dispositivo {}", device.getName());
        }
    }

    private void redirectTraffic(Device device, String switchId, int alternativePortNo) throws Exception {
        String targetIp = device.getIp();
        
        log.info("Body device: {}", new Gson().toJson(device)); // TODO VALIDAR O QUE ESTÁ CHEGANDO AQUI

        String jsonBody = String.format(
                "{\"switch\": \"%s\", \"name\": \"flow-mod\", \"priority\": 10, \"eth_type\": 0x0800, \"ipv4_src\": \"%s\", \"actions\":\"output:%d\"}",
                switchId, targetIp, alternativePortNo
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ryuConfig.getUrl() + "/stats/flowentry/add"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            log.info("Rota alternativa aplicada com sucesso para o dispositivo {}", device.getName());
        } else {
            log.warn("Falha ao aplicar rota alternativa para {}: {}", device.getName(), response.body());
        }
    }

    private double getLatency(String deviceIp) throws Exception {
        String command = "ping -c 10 " + deviceIp;
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        process.waitFor();

        return extractAverageLatency(output.toString());
    }

    private double extractAverageLatency(String pingOutput) {
        Pattern pattern = Pattern.compile("rtt min/avg/max/mdev = (\\d+\\.\\d+)/(\\d+\\.\\d+)/(\\d+\\.\\d+)/(\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(pingOutput);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(2));
        }

        return 0.0;
    }

    private void reduceDataFrequency(Device device) {
        String commandTopic = String.format("iot/sensor/control/%s",
                device.getIp().replace(".", "_"));
        String payload = "{\"action\": \"reduce_frequency\", \"interval\": 10}";

        mqttClient.publish(commandTopic, payload);
        log.info("Reduzindo frequência de envio de dados para o dispositivo {}", device.getName());
    }

    private void prioritizeCriticalTraffic(Device device) throws Exception {
        String targetIp = device.getIp();
        String outputAsResult = qosService.fetchHostTopology();

        String switchId = getSwitchIdForDevice(targetIp, outputAsResult);
        int portNo = getPortForDevice(targetIp, outputAsResult);

        if (switchId == null || portNo == -1) {
            log.warn("Dispositivo com IP {} não encontrado na topologia. Prioridade não aplicada.", targetIp);
            return;
        }

        String jsonBody = String.format(
                "{\"port_name\":\"%s-eth%s\", \"type\": \"linux-htb\", \"max-rate\": \"%d\", \"priority\": \"1\"}",
                switchId, portNo, bandwidthConfig.getBandwidthLimit() * 1000
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ryuConfig.getUrl() + "/qos/queue/0000000000000001"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            log.info("Prioridade de tráfego aplicada com sucesso para o dispositivo {}", device.getName());
        } else {
            log.warn("Falha ao aplicar prioridade de tráfego para {}: {}", device.getName(), response.body());
        }
    }

    private int getAlternativePortNo(String switchId) throws Exception {
        String topologyJson = qosService.fetchHostTopology();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(topologyJson);

        for (JsonNode link : root.path("links")) {
            String srcSwitch = link.path("src").path("dpid").asText();
            String dstSwitch = link.path("dst").path("dpid").asText();

            if (srcSwitch.equals(switchId)) {
                int portNo = link.path("src").path("port_no").asInt();
                log.info("Porta alternativa encontrada para o switch {}: porta {}", switchId, portNo);
                return portNo;
            } else if (dstSwitch.equals(switchId)) {
                int portNo = link.path("dst").path("port_no").asInt();
                log.info("Porta alternativa encontrada para o switch {}: porta {}", switchId, portNo);
                return portNo;
            }
        }

        log.warn("Nenhuma porta alternativa disponível para o switch {}", switchId);
        return -1;
    }

    private String getSwitchIdForDevice(String targetIp, String topologyJson) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(topologyJson);
        
        for (JsonNode host : root) {
        	for (JsonNode ipNode : host.path("ipv4")) {
        		if (ipNode.asText().equals(targetIp)) {
        			return host.path("port").path("dpid").asText();
        		}
        	}
        }

        log.warn("Switch ID não encontrado para o dispositivo com IP {}", targetIp);
        return null;
    }

    private int getPortForDevice(String targetIp, String topologyJson) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(topologyJson);

        for (JsonNode host : root) {
            if (host.path("ip").toString().contains(targetIp)) {
                return host.path("port").path("port_no").asInt();
            }
        }

        log.warn("Porta não encontrada para o dispositivo com IP {}", targetIp);
        return -1;
    }

    private int getPrimaryPortNo(String switchId, String deviceIp) throws Exception {
        String topologyJson = qosService.fetchHostTopology();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(topologyJson);

        int primaryPort = -1;
        boolean isPrimarySet = false;
        
        for (JsonNode host : root) {
            String dpid = host.path("port").path("dpid").asText();
            String ip = "";
            for (JsonNode ipNode : host.path("ipv4")) {
        		if (ipNode.asText().equals(deviceIp)) {
        			ip = ipNode.asText();
        		}
        	}

            int portNo = host.path("port").path("port_no").asInt();

            if (dpid.equals(switchId) && ip.equals(deviceIp)) {
                if (!isPrimarySet) {
                    primaryPort = portNo;
                    isPrimarySet = true;
                    log.info("Porta primária para o dispositivo {} encontrada: porta {}", deviceIp, primaryPort);
                } else {
                    log.info("Link redundante detectado para o dispositivo {}: porta {}", deviceIp, portNo);
                }
            }
        }

        if (primaryPort == -1) {
            log.warn("Nenhuma porta primária encontrada para o dispositivo {} no switch {}", deviceIp, switchId);
        }

        return primaryPort;
    }

}