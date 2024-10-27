//package br.com.ufu.iot_router_backend.routes.orchestrator;
//
//import br.com.ufu.iot_router_backend.processors.PingProcessor;
//import org.apache.camel.builder.RouteBuilder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class PingRoute extends RouteBuilder {
//    @Override
//    public void configure() throws Exception {
//        from("timer://latencyMonitor?period=20000")  // Executa a cada 60 segundos
//                .process(new PingProcessor())
//                .log("Ping Processor executado!");
//    }
//}
