//package br.com.ufu.iot_router_backend.processors.prometheus;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.apache.camel.Exchange;
//import org.apache.camel.Processor;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Component
//public class ContainerCpuUsageProcessor implements Processor {
//
//    private static final String CONTAINER_PREFIX = "mn.mqtt";  // Prefixo para filtrar containers
//    private static final double CPU_CRITICAL_THRESHOLD = 0.8;  // Limite crítico de CPU
//    private static final double CPU_ALERT_THRESHOLD = 0.5;     // Lim
//
//    @Override
//    public void process(Exchange exchange) throws Exception {
//        String body = exchange.getIn().getBody(String.class);
//
//        // Filtrar containers pelo prefixo e processar os dados
//        List<JsonNode> containers = filterContainersByPrefix(body);
//
//        for (JsonNode container : containers) {
//            String containerName = container.path("metric").path("name").asText();
//            double cpuUsage = container.path("value").get(1).asDouble();
//
//            // Lógica de decisão para cada container com o prefixo desejado
//            if (cpuUsage > CPU_CRITICAL_THRESHOLD) {
//                // Uso de CPU crítico, ação imediata de ajuste de QoS
//                String containerIp = getContainerIp(containerName); // Função para obter o IP
//                exchange.getContext().createProducerTemplate().sendBodyAndHeader(
//                        "direct:ajusteQoS",
//                        "Critico - " + containerName,
//                        "ContainerIp", containerIp
//                );
//            } else if (cpuUsage > CPU_ALERT_THRESHOLD) {
//                // Uso de CPU em alerta, apenas loga e monitora
//                exchange.getContext().createProducerTemplate().sendBody("direct:logAlerta", "Alerta - " + containerName);
//            } else {
//                // Uso de CPU normal, manter sem ação
//                exchange.getContext().createProducerTemplate().sendBody("direct:logNormal", "Normal - " + containerName);
//            }
//        }
//    }
//
//    private List<JsonNode> filterContainersByPrefix(String jsonResponse) throws Exception {
//        ObjectMapper mapper = new ObjectMapper();
//        JsonNode root = mapper.readTree(jsonResponse);
//        JsonNode results = root.path("data").path("result");
//        return results.findParents("metric").stream()
//                .filter(node -> node.path("metric").path("name").asText().startsWith(CONTAINER_PREFIX))
//                .collect(Collectors.toList());
//    }
//}
