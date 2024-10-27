package br.com.ufu.iot_router_backend.routes.monitoramento;

import br.com.ufu.iot_router_backend.config.IpFetcherConfig;
import br.com.ufu.iot_router_backend.processors.monitoramento.LarguraBandaProcessor;
import br.com.ufu.iot_router_backend.processors.monitoramento.PingProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LarguraBandaRoute extends RouteBuilder {

    @Autowired
    private IpFetcherConfig ipConfig;

    @Override
    public void configure() throws Exception {
        from("direct:analyzeBandwidth")
                .to(ipConfig.getUrl())
                .log("Devices: ${body}")
                .log("Iniciando ciclo de monitoramento largura de banda.")
                .process(new LarguraBandaProcessor())
                .log("Ciclo de monitoramento de largura de banda finalizado.");
    }
}
