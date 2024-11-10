package br.com.ufu.iot_router_backend.processors.monitoramento;


import br.com.ufu.iot_router_backend.config.BandwidthConfig;
import br.com.ufu.iot_router_backend.config.MyPrometheusConfig;
import br.com.ufu.iot_router_backend.config.RyuConfig;
import br.com.ufu.iot_router_backend.model.Device;
import br.com.ufu.iot_router_backend.model.NetworkState;
import br.com.ufu.iot_router_backend.service.GetIpsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LarguraBandaProcessor implements Processor {

    @Autowired
    private RyuConfig ryuConfig;

    @Autowired
    private MyPrometheusConfig prometheusConfig;

    @Autowired
    private BandwidthConfig bandwidthConfig;

    @Autowired
    private GetIpsService getIpsService;

    private final Map<Device, NetworkState> deviceNetworkStates = new HashMap<>();

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        List<Device> devices = getIpsService.updateDeviceList(body);

        for (Device device : devices) {
            long currentReceiveBytes = getNetworkReceiveBytesTotal(exchange, device.getName());
            long currentTransmitBytes = getNetworkTransmitBytesTotal(exchange, device.getName());

            calculateBandwidth(device, currentReceiveBytes, currentTransmitBytes);
        }
    }

    private List<Device> updateDeviceList(String jsonResponse) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        List<Device> devices = new ArrayList<>();

        for (JsonNode node : root) {
            String name = node.path("name").asText();
            String ip = node.path("ip").asText();
            devices.add(new Device(name, ip));
        }
        return devices;
    }

    private long getNetworkReceiveBytesTotal(Exchange exchange, String containerName) throws Exception {
        log.info("GET container_network_receive_bytes_total para {}", containerName);
        CamelContext context = exchange.getContext();
        ProducerTemplate template = context.createProducerTemplate();

        String prometheusQuery = String.format(
                "query=rate(container_network_receive_bytes_total{name=\"%s\"}[%s])",
                containerName, bandwidthConfig.getRateInterval()
        );

        String response = template.requestBodyAndHeader(
                prometheusConfig.createURI(),
                null,
                Exchange.HTTP_QUERY,
                prometheusQuery,
                String.class
        );

        return parseBytesFromJson(response);
    }

    private long getNetworkTransmitBytesTotal(Exchange exchange, String containerName) throws Exception {
        log.info("GET container_network_transmit_bytes_total para {}", containerName);
        CamelContext context = exchange.getContext();
        ProducerTemplate template = context.createProducerTemplate();

        String prometheusQuery = String.format(
                "query=rate(container_network_transmit_bytes_total{name=\"%s\"}[%s])",
                containerName, bandwidthConfig.getRateInterval()
        );

        String response = template.requestBodyAndHeader(
                prometheusConfig.createURI(),
                null,
                Exchange.HTTP_QUERY,
                prometheusQuery,
                String.class
        );

        return parseBytesFromJson(response);
    }

    private long parseBytesFromJson(String jsonResponse) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode resultNode = root.path("data").path("result").get(0);

        if (resultNode != null && resultNode.has("value")) {
            return resultNode.path("value").get(1).asLong();
        } else {
            log.warn("Métrica não encontrada.");
            return 0;
        }
    }

    private void calculateBandwidth(Device device, long currentReceiveBytes, long currentTransmitBytes) throws IOException, InterruptedException {
        long currentTimestamp = System.currentTimeMillis() / 1000;

        deviceNetworkStates.putIfAbsent(device, new NetworkState(0, 0, 0));
        NetworkState state = deviceNetworkStates.get(device);

        if (state.getPreviousTimestamp() != 0) {
            long interval = currentTimestamp - state.getPreviousTimestamp();
            long receiveRate = (currentReceiveBytes - state.getPreviousReceiveBytes()) / interval;
            long transmitRate = (currentTransmitBytes - state.getPreviousTransmitBytes()) / interval;

            log.info("Dispositivo: {}, Taxa de Recebimento: {} bytes/s, Taxa de Transmissão: {} bytes/s",
                    device.getName(), receiveRate, transmitRate);

            if (receiveRate > bandwidthConfig.getBandwidthLimit() || transmitRate > bandwidthConfig.getBandwidthLimit()) {
                log.warn("Limite de largura de banda excedido para o dispositivo {}: Aplicando QoS", device.getName());
                applyQoS(device);
            }
        }

        state.setPreviousReceiveBytes(currentReceiveBytes);
        state.setPreviousTransmitBytes(currentTransmitBytes);
        state.setPreviousTimestamp(currentTimestamp);
    }

    private void applyQoS(Device device) throws IOException, InterruptedException {
        log.info("Aplicando QoS para o dispositivo {}", device.getName());
        String targetIp = device.getIp();
        String outputAsResult = this.fetchHostTopology();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(outputAsResult);
        String switchId = null;
        int portNo = -1;

        for (JsonNode host : root) {
            if (host.path("ip").toString().contains(targetIp)) {
                switchId = host.path("port").path("dpid").asText();
                portNo = host.path("port").path("port_no").asInt();
                break;
            }
        }

        if (switchId == null || portNo == -1) {
            log.warn("Dispositivo com IP {} não encontrado na topologia. QoS não aplicado.", targetIp);
            return;
        }

        int bandwidthKbps = bandwidthConfig.getBandwidthLimit();
        String jsonBody = String.format(
                "{\"port_name\":\"%s-eth%s\", \"type\": \"linux-htb\", \"max-rate\": \"%d\"}",
                switchId, portNo, bandwidthKbps * 1000
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ryuConfig.getUrl() + "/qos/queue/0000000000000001"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            log.info("QoS aplicado com sucesso para o dispositivo {} (IP: {}): largura de banda limitada a {} Kbps",
                    device.getName(), targetIp, bandwidthKbps);
        } else {
            log.warn("Falha ao aplicar QoS para o dispositivo {}. Status: {}. Resposta: {}",
                    device.getName(), response.statusCode(), response.body());
        }
    }

    private String fetchHostTopology() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ryuConfig.getUrl() + "/v1.0/topology/hosts"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 && !response.body().isEmpty()) {
            return response.body();
        }
        System.out.println("Falha ao obter a topologia: " + response.body());
        return "";
    }
}

