package br.com.ufu.iot_router_backend.processors.monitoramento;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import br.com.ufu.iot_router_backend.config.MonitorConfig;
import br.com.ufu.iot_router_backend.enums.QoSDecision;
import br.com.ufu.iot_router_backend.enums.QoSPriority;
import br.com.ufu.iot_router_backend.model.Device;
import br.com.ufu.iot_router_backend.model.OutputPrometheus;
import br.com.ufu.iot_router_backend.observabilidade.MetricsManager;
import br.com.ufu.iot_router_backend.service.GetIpsService;
import br.com.ufu.iot_router_backend.service.PrometheusClient;
import br.com.ufu.iot_router_backend.service.QoSService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PingProcessor implements Processor {

	@Autowired
	private GetIpsService getIpsService;

	@Autowired
	private QoSService qoSService2;

	@Autowired
	private MonitorConfig monitorConfig;

	@Autowired
	private MetricsManager managerMetrics;
	
	@Autowired
	private PrometheusClient client;
	
	@Autowired
	private CamelContext context;

	@Override
	public void process(Exchange exchange) throws Exception {
		log.info("Monitoramento containers Devices Simulados");
		List<Device> containers = getIpsService.updateDeviceList(exchange.getIn().getBody(String.class));
		execute(containers, exchange);
	}

	private void execute(List<Device> containers, Exchange exchange) throws Exception {
		log.info("execute...");

		Device brokerHost = containers.stream().filter(e -> e.getName().contains("mosq")).findFirst().get();
		
//		var latencyBroker = this.getLatency(brokerHost);
//		managerMetrics.observeLatency(latencyBroker, brokerHost.getName());
//		
//		if (latencyBroker > monitorConfig.getLatencyThreshold()) {
//			this.managerMetrics.incrementBroker(brokerHost.getName());
//		}
		
		List<Device> devices = containers.stream().filter(e -> e.getName().contains("mqtt"))
				.collect(Collectors.toList());
		
        setPriority(devices);
		
		ProducerTemplate template = context.createProducerTemplate();
		double rxBytes = 0;
		double txBytes = 0;
		for (Device device : devices) {
			var ip = device.getIp();
			
			var outRxBytes = getRxBytes(template, device);
			var outTxBytes = getTxBytes(template, device);
			
			if (outTxBytes != null && outTxBytes.getData() != null) {
				if (outTxBytes.getData().getResultMetric() != null && 
						!outTxBytes.getData().getResultMetric().isEmpty()) {
					rxBytes = Double.valueOf(outRxBytes.getData().getResultMetric().get(0).getValue()[1]);
					txBytes = Double.valueOf(outTxBytes.getData().getResultMetric().get(0).getValue()[1]);
				}				
			}
			
			log.info("Métricas coletadas para {}: RX={} bytes, TX={} bytes", device.getName(), rxBytes, txBytes);

			double latency = this.getLatency(device);
			managerMetrics.incrementAvailableDevice(device.getName());
			managerMetrics.observeLatency(latency, device.getName());

			if (latency > monitorConfig.getLatencyThreshold()) {
				log.info("Latência detectada: {}ms para o container mqtt1 (limite: {})", latency,
						monitorConfig.getLatencyThreshold());
				
				managerMetrics.incrementHighLatencyCount(device.getName());
				
				device.setRxBytes(rxBytes);
				device.setTxBytes(txBytes);
				
				var result = qoSService2.analyzeLatencyAndConfigureQueue(device, latency);

				if (result.getQosDecision() == QoSDecision.APPLIED) {
					log.info("A fila foi criada com sucesso.");
					var host = qoSService2.getSwitchAndPortForDevice(ip);
					qoSService2.applyQoSRule(device.getPriority(),
							host.getPort().getDpid(), host.getPort().getPortNo(), ip,
							brokerHost.getIp(), result.getQueueId());

					this.managerMetrics.incrementApplyRule(device.getName());
					managerMetrics.incrementHighLatencyCount(device.getName());
					managerMetrics.incrementRedirectEvents(device.getName());

					String iotDeviceIp = ip;
					String brokerIp = brokerHost.getIp();

					var hostDevice = qoSService2.getSwitchAndPortForDevice(iotDeviceIp);
					var hostBroker = qoSService2.getSwitchAndPortForDevice(brokerIp);
					var links = qoSService2.getLinks();
					String switch2Dpid = hostDevice.getPort().getDpid();
					String switch1Dpid = hostBroker.getPort().getDpid();

					var linkToS1 = links.stream()
							.filter(link -> link.getSrc().getDpid().equals(switch2Dpid)
									&& link.getDst().getDpid().equals(switch1Dpid))
							.findFirst().orElseThrow(() -> new IllegalStateException("Link not found"));

					String s2ToS1Port = linkToS1.getSrc().getPortNo();
					String s1ToBrokerPort = hostBroker.getPort().getPortNo();
					this.managerMetrics.incrementRedirectEvents(device.getName());
					this.managerMetrics.incrementRedirectTraffic(device.getName());	
					qoSService2.addFlowRule(switch2Dpid, iotDeviceIp, brokerIp, s2ToS1Port);
					qoSService2.addFlowRule(switch1Dpid, iotDeviceIp, brokerIp, s1ToBrokerPort);
				}
			}
		}
	}

	private OutputPrometheus getTxBytes(ProducerTemplate template, Device device)
			throws JsonMappingException, JsonProcessingException {
		var qtxBytes = String.format("query=rate(container_network_transmit_bytes_total{name=\"%s\"}[1m])",
				"mn.".concat(device.getName()));
		
		var outTxBytes = client.getMetric(qtxBytes, template);
		return outTxBytes;
	}

	private OutputPrometheus getRxBytes(ProducerTemplate template, Device device)
			throws JsonMappingException, JsonProcessingException {
		var qrxBytes = String.format("query=rate(container_network_receive_bytes_total{name=\"%s\"}[1m])",
				"mn.".concat(device.getName()));
		
		var outRxBytes = client.getMetric(qrxBytes, template);
		return outRxBytes;
	}

	private void setPriority(List<Device> devices) {
		Collections.shuffle(devices);
       
        int halfSize = devices.size() / 2;
       
        for (int i = 0; i < devices.size(); i++) {
            if (i < halfSize) {
                devices.get(i).setPriority(QoSPriority.HIGH);
            } else {
                devices.get(i).setPriority(QoSPriority.LOW);
            }
        }
	}

	private double getLatency(Device device) throws Exception {

		if (monitorConfig.isTestMode()) {
			log.info("Mock latency");
			return 300;
		}

		String command = "ping -c 10 " + device.getIp();
		Process process = Runtime.getRuntime().exec(command);
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

		StringBuilder output = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			output.append(line).append("\n");
		}
		
		int exitCode = process.waitFor();
		
		if (exitCode != 0) {
	        log.warn("PING falhou para o dispositivo com IP {}", device.getIp());
	        device.setAvailable(false);
	        this.managerMetrics.incrementDownDevice(device.getName());
	        return -1; // Indica indisponibilidade
	    }

		return extractAverageLatency(output.toString());
	}

	private double extractAverageLatency(String pingOutput) {
		Pattern pattern = Pattern
				.compile("rtt min/avg/max/mdev = (\\d+\\.\\d+)/(\\d+\\.\\d+)/(\\d+\\.\\d+)/(\\d+\\.\\d+)");
		Matcher matcher = pattern.matcher(pingOutput);

		if (matcher.find()) {
			return Double.parseDouble(matcher.group(2));
		}

		return 0.0;
	}
}