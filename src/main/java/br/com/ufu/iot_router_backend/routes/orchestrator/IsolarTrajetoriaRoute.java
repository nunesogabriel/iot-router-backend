//package br.com.ufu.iot_router_backend.routes.orchestrator;
//
//import org.apache.camel.Exchange;
//import org.apache.camel.builder.RouteBuilder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class IsolarTrajetoriaRoute extends RouteBuilder {
//    @Override
//    public void configure() throws Exception {
//
//        from("direct:isolarTrajetoria")
//                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
//                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
//                .setBody(simple("""
//                            {
//                              "dpid": 1,
//                              "cookie": 0,
//                              "idle_timeout": 0,
//                              "hard_timeout": 0,
//                              "priority": 300,
//                              "match":{
//                                "in_port":1
//                              },
//                              "actions":[
//                                {
//                                  "type":"OUTPUT",
//                                  "port": 3
//                                }
//                              ]
//                            }
//                        """))
//                .to("http://ryu-controller:8080/stats/flowentry/add")
//                .log("Tráfego redirecionado para rota alternativa devido a erros de pacotes.");
//    }
//}
