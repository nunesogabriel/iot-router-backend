//package br.com.ufu.iot_router_backend.routes;
//
//import org.apache.camel.builder.RouteBuilder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class TrafficRoute extends RouteBuilder  {
//    @Override
//    public void configure() throws Exception {
//        from("direct:rerouteTraffic")
//                .log("Rerouting traffic due to high latency: ${header.latency}")
//                .setHeader("inPort", constant("1"))
//                .setHeader("ipv4Src", constant("10.0.0.1"))
//                .setHeader("ipv4Dst", constant("10.0.0.2"))
//                .setHeader("outPort", constant("3"))  // Porta alternativa para redirecionar o tráfego
//                .setBody(simple("{ \"dpid\": 1, \"cookie\": 1, \"priority\": 100, \"match\": {\"in_port\": ${header.inPort}, \"eth_type\": 2048, \"ipv4_dst\": \"${header.ipv4Dst}\", \"ipv4_src\": \"${header.ipv4Src}\" }, \"actions\": [{\"type\": \"OUTPUT\", \"port\": ${header.outPort} }] }"))
//                .to("http://10.0.0.220:8080/stats/flowentry/add")  // Adiciona o fluxo de redirecionamento ao Ryu
//                .log("Traffic rerouted successfully");
//
//    }
//}
