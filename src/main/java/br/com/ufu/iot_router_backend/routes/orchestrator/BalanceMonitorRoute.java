//package br.com.ufu.iot_router_backend.routes.orchestrator;
//
//import br.com.ufu.iot_router_backend.processors.BalanceMonitorProcessor;
//import org.apache.camel.Exchange;
//import org.apache.camel.builder.RouteBuilder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class BalanceMonitorRoute extends RouteBuilder {
//    @Override
//    public void configure() throws Exception {
//        from("timer://balanceMonitor?fixedRate=true&period=10000")
//                .routeId("BALANCE_MONITOR")
//                .to("http://prometheus:9090/api/v1/query?query=rate(container_cpu_usage_seconds_total[1m])")
//                .process(new BalanceMonitorProcessor())
//                .choice()
//                .when(simple("${body} > 0.80"))  // Se o uso de CPU for maior que 80%
//                .to("direct:balancearCarga")  // Balanceia a carga
//                .otherwise()
//                .log("Uso de CPU dentro do limite aceitável.");
//    }
//}
