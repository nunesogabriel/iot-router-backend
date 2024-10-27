//package br.com.ufu.iot_router_backend.routes.orchestrator;
//
//import br.com.ufu.iot_router_backend.processors.LatencyMonitorProcessor;
//import org.apache.camel.Exchange;
//import org.apache.camel.builder.RouteBuilder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class LatencyMonitorRoute extends RouteBuilder {
//    @Override
//    public void configure() throws Exception {
//        from("timer://latencyMonitor?fixedRate=true&period=10000")  // Consulta a cada 5 segundos
//                .routeId("LATENCY_MONITOR")
//                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
//                .toD("http://prometheus:9090/api/v1/query?query=rate(container_network_transmit_packets_total{interface=\"eth0\"}[1m])")
//                .process(new LatencyMonitorProcessor())
//                .choice()
//                    .when(simple("${body} > 0.05"))  // Latência acima de 50ms
//                        .to("direct:ajustarFluxo")  // Executa a ação de ajuste
//                    .otherwise()
//                        .log("Latência dentro do limite aceitável.");
//
//    }
//}
