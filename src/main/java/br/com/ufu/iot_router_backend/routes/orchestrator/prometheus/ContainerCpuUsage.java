package br.com.ufu.iot_router_backend.routes.orchestrator.prometheus;

import br.com.ufu.iot_router_backend.config.MyPrometheusConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
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
public class ContainerCpuUsage extends RouteBuilder {

    private static final String PROMETHEUS_BASE_URL = "http://prometheus:9090";
    private static final String IP_FETCHER_URL = "http://ip_fetcher:5000/get_ips";
    private static final String RYU_BASE_URL = "http://ryu-controller:8080";
    private static final double CPU_CRITICAL_THRESHOLD = 0.8;  // Limite crítico de CPU
    private static final double CPU_ALERT_THRESHOLD = 0.5;     // Limite de alerta de CPU
    private static final int BANDWIDTH_LIMIT = 1000;           // Limite de largura de banda em Kbps
//    private List<Device> devices = new ArrayList<>();          // Lista de dispositivos com nome e IP

    @Autowired
    private MyPrometheusConfig configPrometheus;

    @Override
    public void configure() throws Exception {
        // Rota para atualizar a lista de dispositivos
//        from("timer://updateDeviceList?period=50000") // Atualiza a cada 60 segundos
//                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
//                .to("http://ip-fetcher:5000/get_ips")
//                .process(exchange -> {
//                    String body = exchange.getIn().getBody(String.class);
//                    updateDeviceList(body); // Função para atualizar a lista de dispositivos
//                }).log("Lista de dispositivos atualizada: ${body}");

        // Rota principal para monitorar métricas e aplicar QoS
        from("timer://monitorAndApplyQoS?period=60000")
                .routeId("MONITOR_APPLY_QoS")
                .to("http://ip-fetcher:5000/get_ips")
                .log("Lista de dispositivos: ${body}")
                .process(exchange -> {
                    String body = exchange.getIn().getBody(String.class);
                    List<Device> devices =  updateDeviceList(body);
                    for (Device device : devices) {
                        // Consulta de CPU e Latência de cada dispositivo
                        double cpuUsage = getCpuUsage(device.getName());
                        double latency = getLatency(device.getIp());

                        log.info("O dispositivo {} está com a CPU [{}] e Latência [{}]", device.getName(), cpuUsage, latency);

                        if (cpuUsage > CPU_CRITICAL_THRESHOLD || latency > 250) {
                            applyQoS(device.getIp(), "Critico - CPU ou Latência alta");
                        } else if (cpuUsage > CPU_ALERT_THRESHOLD) {
                            logAlert(device.getName(), "CPU em nível de alerta");
                        } else {
                            logNormal(device.getName(), "CPU em nível normal");
                        }
                    }
                });
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

    private double getCpuUsage(String containerName) throws Exception {
        log.info("GET CPU");
        CamelContext context = getContext();
        ProducerTemplate template = context.createProducerTemplate();

        // Construindo a query PromQL para obter o uso de CPU do container
        String prometheusQuery = String.format(
                "query=rate(container_cpu_usage_seconds_total{name=\"%s\"}[1m])", containerName
        );

        // Executando a requisição ao Prometheus
        String response = template.requestBodyAndHeader(
                PROMETHEUS_BASE_URL + "/api/v1/query",
                null,
                Exchange.HTTP_QUERY,
                prometheusQuery,
                String.class
        );

        return parseCpuUsageFromJson(response);
    }

    private double parseCpuUsageFromJson(String jsonResponse) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        log.info("J = {}", jsonResponse);
        JsonNode root = mapper.readTree(jsonResponse);

        // Acessa o primeiro resultado retornado
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

        return 0.0;  // Caso não consiga extrair a latência
    }

    // Função para aplicar a regra de QoS para o dispositivo com IP fornecido
    private void applyQoS(String ip, String message) {
        String qosRequest = String.format(
                "{ \"qos\": { \"max_rate\": %d, \"ip\": \"%s\" } }", BANDWIDTH_LIMIT * 1000, ip);

        // Envio do comando para o Ryu usando o Camel
        getContext().createProducerTemplate().sendBodyAndHeader(
                RYU_BASE_URL + "/qos/queue/0000000000000001", qosRequest, Exchange.HTTP_METHOD, "POST");

        System.out.println("Aplicada regra de QoS para IP " + ip + ": " + message);
    }

    // Função para logar estado de alerta
    private void logAlert(String containerName, String message) {
        System.out.println("Alerta para " + containerName + ": " + message);
    }

    // Função para logar estado normal
    private void logNormal(String containerName, String message) {
        System.out.println("Normal para " + containerName + ": " + message);
    }

    // Classe para armazenar informações do dispositivo
    public static class Device {
        private final String name;
        private final String ip;

        public Device(String name, String ip) {
            this.name = name;
            this.ip = ip;
        }

        public String getName() {
            return name;
        }

        public String getIp() {
            return ip;
        }
    }
}
