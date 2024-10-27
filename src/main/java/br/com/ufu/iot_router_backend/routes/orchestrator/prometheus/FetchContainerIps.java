//package br.com.ufu.iot_router_backend.routes.orchestrator.prometheus;
//
//import org.apache.camel.Exchange;
//import org.apache.camel.builder.RouteBuilder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class FetchContainerIps extends RouteBuilder {
//    @Override
//    public void configure() throws Exception {
//        from("timer://fetchContainerIps?period=60000")
//                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
//                .to("http://ip-fetcher:5000/get_ips") // URL completa e correta
//                .process(exchange -> {
//                    String body = exchange.getIn().getBody(String.class);
//                    System.out.println("Lista de containers e IPs: " + body);
//                });
//    }
//}
