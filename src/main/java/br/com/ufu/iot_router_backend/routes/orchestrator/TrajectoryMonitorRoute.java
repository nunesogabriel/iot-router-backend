//package br.com.ufu.iot_router_backend.routes.orchestrator;
//
//import br.com.ufu.iot_router_backend.processors.TrajectoryMonitorProcessor;
//import org.apache.camel.builder.RouteBuilder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class TrajectoryMonitorRoute extends RouteBuilder {
//    @Override
//    public void configure() throws Exception {
//        from("timer://trajectoryMonitor?fixedRate=true&period=10000")
//                .routeId("TRAJECTORY_MONITOR")
//                .to("http://prometheus:9090/api/v1/query?query=rate(container_network_receive_errors_total[1m])")
//                .process(new TrajectoryMonitorProcessor())
//                .choice()
//                .when(simple("${body} > 10"))  // Se houver mais de 10 erros de pacotes por minuto
//                .to("direct:isolarTrajetoria")  // Isola a trajetória problemática
//                .otherwise()
//                .log("Erros de pacotes dentro do limite aceitável.");
//    }
//}
