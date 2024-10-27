package br.com.ufu.iot_router_backend.processors.monitoramento;

import br.com.ufu.iot_router_backend.config.MyPrometheusConfig;
import br.com.ufu.iot_router_backend.config.RyuConfig;
import br.com.ufu.iot_router_backend.model.Device;
import br.com.ufu.iot_router_backend.routes.orchestrator.prometheus.ContainerCpuUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CpuProcessor implements Processor {

    private static final int BANDWIDTH_LIMIT = 1000;           // Limite de largura de banda em Kbps
    private static final double CPU_CRITICAL_THRESHOLD = 0.8;  // Limite crítico de CPU
    private static final double CPU_ALERT_THRESHOLD = 0.5;

    @Autowired
    private MyPrometheusConfig prometheusConfig;

    @Autowired
    private RyuConfig ryuConfig;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        List<Device> devices = updateDeviceList(body);
        for (Device device : devices) {

            double cpuUsage = getCpuUsage(exchange, device.getName());
            double latency = getLatency(device.getIp());

            log.info("O dispositivo {} está com a CPU [{}] e Latência [{}]", device.getName(), cpuUsage, latency);

            if (cpuUsage > CPU_CRITICAL_THRESHOLD || latency > 250) {
                applyQoS(exchange, device.getIp(), "Critico - CPU ou Latência alta");
            } else if (cpuUsage > CPU_ALERT_THRESHOLD) {
                log.info("Alerta para {}: CPU em nível de alerta.", device.getName());
            } else {
                log.info("Normal para {}: CPU em nível normal.", device.getName());
            }
        }
    }


    private List<Device> updateDeviceList(String jsonResponse) throws Exception {
        List<Device> devices = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        for (JsonNode node : root) {
            String name = node.path("name").asText();
            String ip = node.path("ip").asText();
            devices.add(new Device(name, ip));
        }

        return devices;
    }

    private double getCpuUsage(Exchange exchange, String containerName) throws Exception {
        log.info("GET CPU");
        CamelContext context = exchange.getContext();
        ProducerTemplate template = context.createProducerTemplate();

        String prometheusQuery = String.format(
                "query=rate(container_cpu_usage_seconds_total{name=\"%s\"}[1m])", containerName
        );

        String response = template.requestBodyAndHeader(
                prometheusConfig.createURI(),
                null,
                Exchange.HTTP_QUERY,
                prometheusQuery,
                String.class
        );

        return parseCpuUsageFromJson(response);
    }

    private double parseCpuUsageFromJson(String jsonResponse) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode resultNode = root.path("data").path("result").get(0);
        if (resultNode != null && resultNode.has("value")) {
            return resultNode.path("value").get(1).asDouble(); // Retorna o valor de CPU
        } else {
            System.out.println("Métrica de CPU não encontrada para o container.");
            return 0.0;
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
            return Double.parseDouble(matcher.group(2));  // Latência média (segundo valor)
        }
        return 0.0;
    }

    private void applyQoS(Exchange exchange, String ip, String message) {
        String qosRequest = String.format(
                "{ \"qos\": { \"max_rate\": %d, \"ip\": \"%s\" } }", BANDWIDTH_LIMIT * 1000, ip);
        exchange.getContext().createProducerTemplate().sendBodyAndHeader(
                ryuConfig.getApi() + "/qos/queue/0000000000000001",
                qosRequest, Exchange.HTTP_METHOD, "POST");
        log.info("Aplicada regra de QoS para IP {}: {}", ip, message);
    }
}
