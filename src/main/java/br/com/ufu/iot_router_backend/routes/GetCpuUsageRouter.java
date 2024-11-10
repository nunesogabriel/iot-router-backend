//package br.com.ufu.iot_router_backend.routes;
//
//import org.apache.camel.builder.RouteBuilder;
//import org.apache.camel.model.dataformat.JsonLibrary;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Map;
//
//@Component
//public class GetCpuUsageRouter extends RouteBuilder {
//    @Override
//    public void configure() throws Exception {
//        from("timer://cpuCheck?period=5000")
//                .to("http://172.20.0.10:9090/api/v1/query?query=rate(container_cpu_usage_seconds_total[1m])")// Executa a cada 1 minuto
//                .unmarshal().json(JsonLibrary.Jackson) // Converte a resposta JSON
//                .process(exchange -> {
//                    // Obtenha o corpo da resposta
//                    Map<String, Object> body = exchange.getIn().getBody(Map.class);
//                    Map<String, Object> data = (Map<String, Object>) body.get("data");
//                    List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("result");
//
//                    // Itera sobre os resultados
//                    for (Map<String, Object> result : results) {
//                        Map<String, Object> metric = (Map<String, Object>) result.get("metric");
//                        String containerName = (String) metric.get("name");  // Nome do container
//
//                        // O valor do uso de CPU está na lista de "value"
//                        List<Object> value = (List<Object>) result.get("value");
//                        Double cpuUsage = Double.parseDouble(value.get(1).toString());  // Uso de CPU em percentual
//
//                        // Processa a informação de uso de CPU (exemplo: log ou redistribuir carga)
//                        System.out.println("Container: " + containerName + " Uso de CPU: " + cpuUsage);
//
//                        // Ação com base no uso de CPU (por exemplo, redistribuir a carga)
//                        if (cpuUsage > 0.8) {
////                            exchange.getContext().createProducerTemplate().sendBody("direct:redistribute", containerName);
//                        }
//                    }
//                });
//    }
//}