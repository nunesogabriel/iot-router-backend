package br.com.ufu.iot_router_backend.processors.monitoramento;


import br.com.ufu.iot_router_backend.config.MyPrometheusConfig;
import br.com.ufu.iot_router_backend.config.RyuConfig;
import br.com.ufu.iot_router_backend.model.Device;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class LarguraBandaProcessor implements Processor {

    private static final int BANDWIDTH_LIMIT = 1000;           // Limite de largura de banda em Kbps
    private long previousReceiveBytes = 0;
    private long previousTransmitBytes = 0;
    private long previousTimestamp = 0;

    @Autowired
    private MyPrometheusConfig prometheusConfig;

    @Autowired
    private RyuConfig ryuConfig;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        List<Device> devices = updateDeviceList(body);

        for (Device device : devices) {
            long currentReceiveBytes = getNetworkReceiveBytesTotal(exchange, device.getName());
            long currentTransmitBytes = this.getNetworkTransmitBytesToal(exchange, device.getName());
            calculateBandwidth(currentReceiveBytes, currentTransmitBytes);
        }
    }

    private List<Device> updateDeviceList(String jsonResponse) throws Exception {
        List<Device> devices = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        for (JsonNode node : root) {
            String name = node.path("name").asText();
            String ip = node.path("ip").asText();
            devices.add(new Device(name, ip));
        }

        return devices;
    }

    private long getNetworkReceiveBytesTotal(Exchange exchange, String containerName) throws Exception {
        log.info("GET container_network_receive_bytes_total");
        CamelContext context = exchange.getContext();
        ProducerTemplate template = context.createProducerTemplate();

        String prometheusQuery = String.format(
                "query=rate(container_network_receive_bytes_total{name=\"%s\"}[1m])", containerName
        );

        String response = template.requestBodyAndHeader(
                prometheusConfig.createURI(),
                null,
                Exchange.HTTP_QUERY,
                prometheusQuery,
                String.class
        );

        return parseBytesFromJson(response);
    }

    private long getNetworkTransmitBytesToal(Exchange exchange, String containerName) throws Exception {
        log.info("GET container_network_transmit_bytes_total");
        CamelContext context = exchange.getContext();
        ProducerTemplate template = context.createProducerTemplate();

        String prometheusQuery = String.format(
                "query=rate(container_network_transmit_bytes_total{name=\"%s\"}[1m])", containerName
        );

        String response = template.requestBodyAndHeader(
                prometheusConfig.createURI(),
                null,
                Exchange.HTTP_QUERY,
                prometheusQuery,
                String.class
        );

        return parseBytesFromJson(response);
    }

    private long parseBytesFromJson(String jsonResponse) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode resultNode = root.path("data").path("result").get(0);

        // Verifica se a métrica foi encontrada
        if (resultNode != null && resultNode.has("value")) {
            return resultNode.path("value").get(1).asLong();
        } else {
            System.out.println("Métrica não encontrada.");
            return 0;
        }
    }

    private void calculateBandwidth(long currentReceiveBytes, long currentTransmitBytes) {
        long currentTimestamp = System.currentTimeMillis() / 1000; // Timestamp em segundos
        if (previousTimestamp != 0) {
            // Calcula a diferença em bytes e o intervalo de tempo
            long receiveRate = (currentReceiveBytes - previousReceiveBytes) / (currentTimestamp - previousTimestamp);
            long transmitRate = (currentTransmitBytes - previousTransmitBytes) / (currentTimestamp - previousTimestamp);

            System.out.println("Taxa de Recebimento (Receive Rate): " + receiveRate + " bytes/s");
            System.out.println("Taxa de Transmissão (Transmit Rate): " + transmitRate + " bytes/s");
        }

        // Atualiza os valores anteriores para a próxima iteração
        previousReceiveBytes = currentReceiveBytes;
        previousTransmitBytes = currentTransmitBytes;
        previousTimestamp = currentTimestamp;
    }
}
