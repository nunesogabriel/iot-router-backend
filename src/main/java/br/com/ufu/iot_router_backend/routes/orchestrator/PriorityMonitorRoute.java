//package br.com.ufu.iot_router_backend.routes.orchestrator;
//
//import br.com.ufu.iot_router_backend.processors.LatencyMonitorProcessor;
//import org.apache.camel.builder.RouteBuilder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class PriorityMonitorRoute extends RouteBuilder {
//    @Override
//    public void configure() throws Exception {
//        from("timer://priorityMonitor?fixedRate=true&period=10000")
//                .routeId("PRIORITY_MONITOR")
//                .to("http://172.20.0.10:9090/api/v1/query?query=rate(node_network_transmit_latency_seconds_total[1m])")
//                .process(new LatencyMonitorProcessor())
//                .choice()
//                .when(simple("${body} > 0.08"))  // Se a latência for maior que 80ms
//                .to("direct:ajustarPrioridade")  // Aumenta a prioridade do tráfego
//                .otherwise()
//                .log("Latência dentro do limite aceitável.");
//
//    }
//}
