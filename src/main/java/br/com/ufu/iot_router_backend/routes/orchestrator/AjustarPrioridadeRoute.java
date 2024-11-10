//package br.com.ufu.iot_router_backend.routes.orchestrator;
//
//import org.apache.camel.Exchange;
//import org.apache.camel.builder.RouteBuilder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class AjustarPrioridadeRoute extends RouteBuilder {
//    @Override
//    public void configure() throws Exception {
//        from("direct:ajustarPrioridade")
//                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
//                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
//                .setBody(simple("""
//                            {
//                              "dpid": 1,
//                              "cookie": 0,
//                              "idle_timeout": 0,
//                              "hard_timeout": 0,
//                              "priority": 50000,
//                              "match":{
//                                "in_port":1
//                              },
//                              "actions":[
//                                {
//                                  "type":"OUTPUT",
//                                  "port": 2
//                                }
//                              ]
//                            }
//                        """))
//                .to("http://10.0.0.220:8080/stats/flowentry/add")
//                .log("Prioridade ajustada para tráfego crítico.");
//    }
//}
