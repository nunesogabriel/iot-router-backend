package br.com.ufu.iot_router_backend.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;

import br.com.ufu.iot_router_backend.config.MonitorConfig;
import br.com.ufu.iot_router_backend.config.RyuConfig;
import br.com.ufu.iot_router_backend.enums.QoSDecision;
import br.com.ufu.iot_router_backend.enums.QoSPriority;
import br.com.ufu.iot_router_backend.model.CreateQueue;
import br.com.ufu.iot_router_backend.model.Device;
import br.com.ufu.iot_router_backend.model.Host;
import br.com.ufu.iot_router_backend.model.Link;
import br.com.ufu.iot_router_backend.model.Qos;
import br.com.ufu.iot_router_backend.observabilidade.MetricsManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class QoSService {

	@Autowired
	private RyuConfig ryuConfig;

	@Autowired
	private MonitorConfig monitorConfig;

	@Autowired
	private MetricsManager managerMetrics;

	public Qos analyzeLatencyAndConfigureQueue(Device device, double latency) {
		if (latency > monitorConfig.getLatencyThreshold()) {
			log.info("Aplicando QoS para o dispositivo: {}", device.getName());
			applyQoS(device, QoSPriority.HIGH);
			return new Qos(QoSDecision.APPLIED, QoSPriority.HIGH.getQueueId());
		} else {
			log.info("Latência dentro do limite aceitável para IP: {}", device.getIp());
			return new Qos(QoSDecision.NORMAL, QoSPriority.HIGH.getQueueId());
		}
	}

	public void addFlowRule(String dpid, String srcIp, String dstIp, String outPort) throws Exception {
		managerMetrics.incrementRyuIterations();
		var url = this.ryuConfig.getUrl() + "/stats/flowentry/add";

		Map<String, Object> flowEntry = Map.of("dpid", Long.parseLong(dpid, 16), "priority", 300, "match",
				Map.of("eth_type", 2048, "ipv4_src", srcIp, "ipv4_dst", dstIp), "actions",
				List.of(Map.of("type", "OUTPUT", "port", Integer.parseInt(outPort))));

		ObjectMapper mapper = new ObjectMapper();
		String flowEntryJson = mapper.writeValueAsString(flowEntry);

		log.info("Redirecionamento de fluxo : {}", flowEntryJson);

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(flowEntryJson)).build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() == 200) {
			System.out.println("Regra adicionada com sucesso no switch " + dpid);
		} else {
			System.out.println("Erro ao adicionar regra no switch " + dpid + ": " + response.body());
		}
	}

	public void applyQoS(Device device, QoSPriority priority) {

		try {

			Host host = getSwitchAndPortForDevice(device.getIp());

			log.info("Informações do dispositivo: {}", new Gson().toJson(host));

			if (host == null) {
				log.warn("Informações de rede não encontradas para o dispositivo com IP {}", device.getIp());
				return;
			}

			String switchId = host.getPort().getDpid();
			String portName = host.getPort().getName();

			this.managerMetrics.incrementCreateQueue(device.getName());

			double rate = Math.max((device.getRxBytes() + device.getTxBytes()) * 0.8, 500000);
			double ceil = rate * 1.2;

			if (device.getPriority() == QoSPriority.HIGH) {
				ceil = rate * 1.5;
			}

			CreateQueue queue = new CreateQueue();
			queue.setMinRate(rate);
			queue.setMaxRate(ceil);
			queue.setSwitchId(switchId);
			queue.setPortName(portName);
			queue.setQueueId(priority.getQueueId());

			createQueue(queue);
			log.info("QoS aplicada ao dispositivo IP {} com prioridade {}", device.getIp(), priority);
		} catch (Exception e) {
			log.error("Erro ao aplicar QoS para o dispositivo com IP {}", device.getIp(), e);
		}
	}

	public void createQueue(CreateQueue queue) {
		String ryuUrl = ryuConfig.getUrl() + "/qos/queue/" + queue.getSwitchId();

		managerMetrics.incrementRyuIterations();

		log.info("Endpoint ryu para criação de filas: {}", ryuUrl);

		Map<String, Object> queueConfig = Map.of("port_name", queue.getPortName(), "type", "linux-htb", "queues",
				List.of(Map.of("queue_id", queue.getQueueId(), "max_rate", String.valueOf(queue.getMaxRate()),
						"min_rate", String.valueOf(queue.getMinRate()))));

		try {

			ObjectMapper objectMapper = new ObjectMapper();
			String queueConfigJson = objectMapper.writeValueAsString(queueConfig);

			log.info("Criando fila de QoS no switch {}: {}", queue.getSwitchId(), queueConfigJson);

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ryuUrl))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(queueConfigJson)).build();

			HttpResponse<String> response = HttpClient.newHttpClient().send(request,
					HttpResponse.BodyHandlers.ofString());

			log.info("Resultado criação de fila: {}", response.body());
		} catch (Exception e) {
			log.error("Erro ao criar fila de QoS no switch {}", queue.getSwitchId(), e);
		}
	}

	public Host getSwitchAndPortForDevice(String deviceIp) {

		try {

			List<Host> hosts = this.fetchHostTopology();

			System.out.println(new Gson().toJson(hosts));

			for (Host host : hosts) {
				for (String ip : host.getIpv4()) {
					if (ip.equals(deviceIp)) {
						return host;
					}
				}
			}

		} catch (Exception e) {
			log.error("Erro ao recuperar informações de rede para o IP {}", deviceIp, e);
		}

		return null;
	}

	public List<Link> getLinks() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		var url = ryuConfig.getUrl() + "/v1.0/topology/links";
		log.info("URL Links Topology: {}", url);
		HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() == 200 && !response.body().isEmpty()) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			List<Link> links = mapper.readValue(response.body(), new TypeReference<List<Link>>() {
			});
			return links;
		}

		log.warn("Falha ao obter a topologia de hosts: {}", response.body());
		return new ArrayList<>();
	}

	public List<Host> fetchHostTopology() throws Exception {
		managerMetrics.incrementRyuIterations();
		HttpClient client = HttpClient.newHttpClient();
		var url = ryuConfig.getUrl() + "/v1.0/topology/hosts";
		log.info("URL Topology: {}", url);
		HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() == 200 && !response.body().isEmpty()) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			List<Host> hosts = mapper.readValue(response.body(), new TypeReference<List<Host>>() {
			});
			return hosts;
		}

		log.warn("Falha ao obter a topologia de hosts: {}", response.body());
		return new ArrayList<>();
	}

	public void applyQoSRule(QoSPriority priority, String switchId, String portNumber, String srcIp, String dstIp,
			int queueId) {

		managerMetrics.incrementRyuIterations();
		managerMetrics.updateQoSApplications(1);

		String ryuUrl = ryuConfig.getUrl() + "/qos/rules/" + switchId;
		int decimalPort = Integer.parseInt(portNumber, 16);

		int valuePriority = priority == QoSPriority.HIGH ? 300 : 150;

		log.info("Prioridade do dispositivo {} é {}", srcIp, valuePriority);

		Map<String, Object> qosRule = Map.of("priority", valuePriority, "match",
				Map.of("in_port", decimalPort, "eth_type", 0x0800, "ip_proto", 6, "nw_src", srcIp, "nw_dst", dstIp),
				"actions", Map.of("queue", queueId));

		try {

			ObjectMapper objectMapper = new ObjectMapper();
			String qosRuleJson = objectMapper.writeValueAsString(qosRule);

			log.info("Aplicando regra de QoS no switch {}: {}", switchId, qosRuleJson);

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ryuUrl))
					.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(qosRuleJson))
					.build();

			HttpResponse<String> response = HttpClient.newHttpClient().send(request,
					HttpResponse.BodyHandlers.ofString());
			log.info("Resposta do Ryu para regra de QoS: {}", response.body());
		} catch (Exception e) {
			log.error("Erro ao aplicar regra de QoS no switch {}", switchId, e);
		}
	}

	public void redirectTraffic(String switchId, String ipSrc, String ipDst, String newPort) {
		managerMetrics.incrementRyuIterations();
		managerMetrics.updateQoSApplications(1);
		String ryuUrl = ryuConfig.getUrl() + "/stats/flowentry/add";

		Map<String, Object> flowData = Map.of("dpid", Long.valueOf(switchId), "priority", 100, "match",
				Map.of("eth_type", 0x0800, "ipv4_src", ipSrc, "ipv4_dst", ipDst), "actions",
				List.of(Map.of("type", "OUTPUT", "port", Integer.parseInt(newPort))));

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			String flowEntryJson = objectMapper.writeValueAsString(flowData);

			log.info("Regra de redirecionamento no switch {}: {}", switchId, flowEntryJson);

			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(ryuUrl))
					.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(flowEntryJson))
					.build();

			HttpResponse<String> response = HttpClient.newHttpClient().send(request,
					HttpResponse.BodyHandlers.ofString());
			log.info("Resposta do Ryu para redirecionamento: {}", response.body());
		} catch (Exception e) {
			log.error("Erro ao redirecionar tráfego no switch {}", switchId, e);
		}
	}
}
