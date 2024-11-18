package br.com.ufu.iot_router_backend.routes.monitoramento;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.ufu.iot_router_backend.config.IpFetcherConfig;
import br.com.ufu.iot_router_backend.processors.monitoramento.CpuProcessor;
import br.com.ufu.iot_router_backend.processors.monitoramento.PingProcessor;

@Component
public class AggregateRoute extends RouteBuilder {

	@Autowired
	private IpFetcherConfig ipConfig;

	@Autowired
	private CpuProcessor cpuProcessor;
	
	 @Autowired
	 private PingProcessor pingProcessor;

	@Override
	public void configure() throws Exception {
		from("direct:checkDevices")
			.routeId("CHECK DEVICES")
			.to(ipConfig.getUrl())
			.log("Iniciando ciclo de monitoramento CPU.")
			.process(cpuProcessor)
			.log("Ciclo de monitoramento de CPU finalizado.")
			.log("Iniciando ciclo latencia")
			.process(pingProcessor)
			.log("Fim ciclo latencia");
	}

}
