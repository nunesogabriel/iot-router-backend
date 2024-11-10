package br.com.ufu.iot_router_backend.routes.monitoramento;//package br.com.ufu.iot_router_backend.routes.orchestrator;

import br.com.ufu.iot_router_backend.config.IpFetcherConfig;
import br.com.ufu.iot_router_backend.processors.monitoramento.PingProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LatenciaRoute extends RouteBuilder {

    @Autowired
    private IpFetcherConfig ipConfig;

    @Autowired
    private PingProcessor pingProcessor;

    @Override
    public void configure() throws Exception {
        from("direct:analyzeLatency")
                .to(ipConfig.getUrl())
                .log("Iniciando ciclo de monitoramento latencia.")
                .process(pingProcessor)
                .log("Ciclo de monitoramento de latencia finalizado.");
    }
}
