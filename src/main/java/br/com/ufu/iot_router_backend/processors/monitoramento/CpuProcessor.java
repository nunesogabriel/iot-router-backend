package br.com.ufu.iot_router_backend.processors.monitoramento;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.paho.PahoConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ufu.iot_router_backend.config.MonitorConfig;
import br.com.ufu.iot_router_backend.config.MyPrometheusConfig;
import br.com.ufu.iot_router_backend.model.Device;
import br.com.ufu.iot_router_backend.observabilidade.MetricsManager;
import br.com.ufu.iot_router_backend.service.GetIpsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CpuProcessor implements Processor {

	@Autowired
	private GetIpsService getIpsService;

	@Autowired
	private MonitorConfig monitorConfig;

	@Autowired
	private MyPrometheusConfig prometheusConfig;

	@Autowired
	private CamelContext camelContext;
	
	@Autowired
	private MetricsManager managerMetrics;

	@Override
	public void process(Exchange exchange) throws Exception {
		String body = exchange.getIn().getBody(String.class);
		List<Device> devices = getIpsService.updateDeviceList(body);

		for (Device device : devices) {
			double cpuUsage = getCpuUsage(device.getName());
			log.info("O dispositivo {} está com uso de CPU em [{}]", device.getName(), cpuUsage);

			if (cpuUsage >= monitorConfig.getCpuCritialThreshold()) {
				log.warn("Uso de CPU crítico para o dispositivo {}. Reduzindo frequência de coleta de dados.",
						device.getName());
				sendMqttCommand(device, "{ \"action\": \"reduce_frequency\", \"interval\": \"25\" }");
			} else {
				log.info("Uso de CPU em nível aceitável para o dispositivo {}.", device.getName());
			}
		}
	}

	private double getCpuUsage(String containerName) throws Exception {
		ProducerTemplate template = camelContext.createProducerTemplate();

		String prometheusQuery = String.format("query=rate(container_cpu_usage_seconds_total{name=\"%s\"}[1m])",
				"mn.".concat(containerName));

		String response = template.requestBodyAndHeader(prometheusConfig.createURI(), null, Exchange.HTTP_QUERY,
				prometheusQuery, String.class);

		return parseCpuUsageFromJson(response);
	}

	private double parseCpuUsageFromJson(String jsonResponse) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(jsonResponse);
		JsonNode resultNode = root.path("data").path("result").get(0);
		if (resultNode != null && resultNode.has("value")) {
			return resultNode.path("value").get(1).asDouble();
		} else {
			System.out.println("Métrica de CPU não encontrada para o container.");
			return 0.0;
		}
	}

	private void sendMqttCommand(Device device, String command) {
		managerMetrics.incrementSendCommandDevice(device.getName());
		log.info("Comando {} foi enviado para o dispotivo: {}", command, device.getName());
		String commandTopic = String.format("iot/sensor/control/%s", device.getIp().replace(".", ""));

		camelContext.createProducerTemplate().sendBodyAndHeader(
				"paho:" + commandTopic + "?brokerUrl=tcp://10.0.0.237:1883", command, PahoConstants.MQTT_QOS, 1);

		log.info("Comando MQTT enviado para o dispositivo {} no tópico {}", device.getName(), commandTopic);
	}
}