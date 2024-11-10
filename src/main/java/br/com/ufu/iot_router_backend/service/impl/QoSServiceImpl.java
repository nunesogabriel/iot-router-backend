package br.com.ufu.iot_router_backend.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import br.com.ufu.iot_router_backend.config.BandwidthConfig;
import br.com.ufu.iot_router_backend.config.RyuConfig;
import br.com.ufu.iot_router_backend.enums.QoSAdjustmentTypeEnum;
import br.com.ufu.iot_router_backend.model.Device;
import br.com.ufu.iot_router_backend.service.QoSService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class QoSServiceImpl implements QoSService {

    @Autowired
    private BandwidthConfig bandwidthConfig;
    
    @Autowired
    private RyuConfig ryuConfig;

    @Autowired
    private MeterRegistry meterRegistry;

    // Contadores e gauges para monitoramento de QoS
    private final Map<String, Counter> qosAdjustmentCounters = new ConcurrentHashMap<>();
    private final Map<String, Gauge> qosBandwidthGauges = new ConcurrentHashMap<>();

    @Override
    public void applyQoS(Device device, QoSAdjustmentTypeEnum adjustmentType) throws Exception {
        String targetIp = device.getIp();
        String outputAsResult = fetchHostTopology();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(outputAsResult);
        String switchId = null;
        int portNo = -1;

        for (JsonNode host : root) {
            if (host.path("ip").toString().contains(targetIp)) {
                switchId = host.path("port").path("dpid").asText();
                portNo = host.path("port").path("port_no").asInt();
                break;
            }
        }

        if (switchId == null || portNo == -1) {
            log.warn("Dispositivo com IP {} não encontrado na topologia. QoS não aplicado.", targetIp);
            return;
        }

        String jsonBody;
        double bandwidthValue = bandwidthConfig.getBandwidthLimit() * 1000;

        if (adjustmentType == QoSAdjustmentTypeEnum.LATENCY) {
            jsonBody = String.format(
                    "{\"port_name\":\"%s-eth%s\", \"type\": \"linux-htb\", \"max-rate\": \"%d\", \"priority\": \"1\"}",
                    switchId, portNo, (int) bandwidthValue
            );
            log.info("Aplicando QoS priorizando baixa latência para o dispositivo {}", device.getName());
        } else {
            jsonBody = String.format(
                    "{\"port_name\":\"%s-eth%s\", \"type\": \"linux-htb\", \"max-rate\": \"%d\"}",
                    switchId, portNo, (int) bandwidthValue
            );
            log.info("Aplicando QoS limitando largura de banda para o dispositivo {}", device.getName());
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ryuConfig.getUrl() + "/qos/queue/0000000000000001"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            log.info("QoS aplicado com sucesso para o dispositivo {} (IP: {}), tipo de ajuste: {}",
                    device.getName(), targetIp, adjustmentType);
            incrementQoSCounter(device);
            updateBandwidthGauge(device, bandwidthValue);
        } else {
            log.warn("Falha ao aplicar QoS para o dispositivo {}. Status: {}. Resposta: {}",
                    device.getName(), response.statusCode(), response.body());
        }
    }

    private void incrementQoSCounter(Device device) {
        String deviceId = device.getName();
        qosAdjustmentCounters.computeIfAbsent(deviceId, id ->
                Counter.builder("container_qos_adjustment_count")
                        .tag("container_id", id)
                        .register(meterRegistry)
        ).increment();
    }

    private void updateBandwidthGauge(Device device, double bandwidthValue) {
        String deviceId = device.getName();
        qosBandwidthGauges.computeIfAbsent(deviceId, id ->
                Gauge.builder("container_qos_last_bandwidth", () -> bandwidthValue)
                        .tag("container_id", id)
                        .register(meterRegistry)
        );
    }

    public void checkStatsSwitch() throws URISyntaxException, IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();
        var url = ryuConfig.getUrl() + "stats/switches";
        log.info("URL Switchs: {}", url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 && !response.body().isEmpty()) {
        	log.info("Result stats_switches = {}", new Gson().toJson(response.body()));
        }   
    }
    
    @Override
    public String fetchHostTopology() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        var url = ryuConfig.getUrl() + "/v1.0/topology/hosts";
        log.info("URL Topology: {}", url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

//        System.out.println("Status code get_topology: " + response.statusCode());
//        System.out.println("Status code get_topology: " + response.uri());

        if (response.statusCode() == 200 && !response.body().isEmpty()) {
//        	log.info("Result = {}", new Gson().toJson(response.body()));
            return response.body();
        }
        log.warn("Falha ao obter a topologia de hosts: {}", response.body());
        return "";
    }
}