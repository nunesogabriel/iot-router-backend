package br.com.ufu.iot_router_backend.routes.monitoramento;

import br.com.ufu.iot_router_backend.config.IpFetcherConfig;
import br.com.ufu.iot_router_backend.processors.monitoramento.CpuProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CpuRoute extends RouteBuilder {

    @Autowired
    private IpFetcherConfig ipConfig;

    @Override
    public void configure() throws Exception {
        from("direct:analyzeCPU")
                .routeId("AnalyzeCPU-Route")
                .to(ipConfig.getUrl())
                .log("Devices: ${body}")
                .log("Iniciando ciclo de monitoramento CPU.")
                .process(new CpuProcessor())
                .log("Ciclo de monitoramento de CPU finalizado.");
    }
}
