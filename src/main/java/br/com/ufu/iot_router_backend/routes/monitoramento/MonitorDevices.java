package br.com.ufu.iot_router_backend.routes.monitoramento;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class MonitorDevices extends RouteBuilder {

    @Override
    public void configure() throws Exception {
//        from("timer://monitorCpu?period=60000")
//                .log("Invoke monitor CPU.")
//                .to("direct:analyzeCPU")
//                .log("Analyze CPU finished.");

//        from("timer://monitorMemoria?period=20000")
//                .log("Invoke monitor memoria.")
//                .to("direct:analyzeMemory")
//                .log("Analyze Memory finished.");

        from("timer://monitorLatencia?period=50000")
                .log("Invoke MONITOR.")
                .to("direct:checkDevices")
                .log("Analyze finished.");

//        from("timer://monitorLarguraBanda?period=20000")
//                .log("Invoke monitor largura banda.")
//                .to("direct:analyzeBandwidth")
//                .log("Analyze Bandwidth finished.");
    }
}
