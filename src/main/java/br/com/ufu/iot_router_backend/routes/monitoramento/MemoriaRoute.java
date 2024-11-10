//package br.com.ufu.iot_router_backend.routes.monitoramento;
//
//import br.com.ufu.iot_router_backend.config.IpFetcherConfig;
//import br.com.ufu.iot_router_backend.processors.monitoramento.CpuProcessor;
//import br.com.ufu.iot_router_backend.processors.monitoramento.MemoriaProcessor;
//import org.apache.camel.builder.RouteBuilder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//@Component
//public class MemoriaRoute extends RouteBuilder {
//
//    @Autowired
//    private IpFetcherConfig ipConfig;
//
//    @Autowired
//    private MemoriaProcessor memoriaProcessor;
//
//    @Override
//    public void configure() throws Exception {
//        from("direct:analyzeMemory")
//                .routeId("AnalyzeMemory-Route")
//                .to(ipConfig.getUrl())
//                .log("Iniciando ciclo de monitoramento memoria.")
//                .process(memoriaProcessor)
//                .log("Ciclo de monitoramento de memoria finalizado.");
//    }
//}
