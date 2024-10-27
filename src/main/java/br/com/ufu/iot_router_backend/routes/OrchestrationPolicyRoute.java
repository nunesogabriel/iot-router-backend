//package br.com.ufu.iot_router_backend.routes;
//
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import org.apache.camel.builder.RouteBuilder;
//
//public class OrchestrationPolicyRoute extends RouteBuilder {
//
//    @Override
//    public void configure() throws Exception {
//        // Rota que se inscreve no tópico de métricas
//        from("paho-mqtt5://iot/metrics")
//                .process(exchange -> {
//                    // Processar métricas recebidas
//                    String payload = exchange.getIn().getBody(String.class);
//                    JsonObject metrics = JsonParser.parseString(payload).getAsJsonObject();
//
//                    long bandwidthUsed = metrics.get("bandwidthUsed").getAsLong();
//                    long latency = metrics.get("latency").getAsLong();
//                    String deviceId = metrics.get("deviceId").getAsString();
//                    String priority = metrics.get("priority").getAsString();  // Prioridade do dispositivo
//
//                    // Aplicar política de orquestração
//                    if (priority.equals("HIGH") && bandwidthUsed > 100000) {
//                        log.info("Dispositivo {} está com alta prioridade e está usando muita largura de banda", deviceId);
//                        // Tomar ação para redistribuir largura de banda ou priorizar esse dispositivo
//                        // Exemplo: aumentar a alocação de banda para o dispositivo
//                    } else if (latency > 100) {
//                        log.info("Dispositivo {} está com alta latência: {} ms", deviceId, latency);
//                        // Tomar ação corretiva para reduzir a latência
//                        // Exemplo: redirecionar tráfego ou ajustar QoS
//                    }
//                })
//                .to("log:orchestration");
//    }
//}