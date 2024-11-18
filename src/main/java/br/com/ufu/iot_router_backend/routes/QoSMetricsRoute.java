package br.com.ufu.iot_router_backend.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.ufu.iot_router_backend.observabilidade.QoSMetricsManager;

@Component
public class QoSMetricsRoute extends RouteBuilder {

    @Autowired
    private QoSMetricsManager metricsService;

    @Override
    public void configure() throws Exception {
        from("timer:metricsTimer?period=45000")
            .routeId("qosMetricsRoute")
            .log("Atualizando métricas de QoS...")
            .process(exchange -> {
                metricsService.updateMetricsForAllSwitches();
            })
            .log("Métricas de QoS atualizadas com sucesso.")
            .end();
    }
}
