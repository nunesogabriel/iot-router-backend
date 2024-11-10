package br.com.ufu.iot_router_backend.processors.monitoramento;

import br.com.ufu.iot_router_backend.config.MonitorConfig;
import br.com.ufu.iot_router_backend.config.MyPrometheusConfig;
import br.com.ufu.iot_router_backend.model.Device;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.paho.PahoConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class MemoriaProcessor implements Processor {

    @Autowired
    private MonitorConfig monitorConfig;

    @Autowired
    private MyPrometheusConfig prometheusConfig;

    @Autowired
    private CamelContext camelContext;

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        List<Device> devices = updateDeviceList(body);
        devices.sort(Comparator.comparingDouble((Device d) -> {
            try {
                return getMemoryUsage(exchange, d.getName());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).reversed());

        for (Device device : devices) {
            double memoryUsage = getMemoryUsage(exchange, device.getName());

            log.info("O dispositivo {} está com uso de Memória em [{}]", device.getName(), memoryUsage);

            if (memoryUsage > monitorConfig.getMemoryThresholdCritical()) {
                log.warn("Uso de Memória crítico para o dispositivo {}. Reinicializando dispositivo.", device.getName());
                sendMqttCommand(device, "{ \"action\": \"restart\" }");
            } else if (memoryUsage > monitorConfig.getMemoryThresholdAlert()) {
                log.info("Uso de Memória em alerta para {}. Reduzindo frequência de coleta de dados.", device.getName());
                sendMqttCommand(device, "{ \"action\": \"reduce_frequency\", \"interval\": \"300\" }");
            } else {
                log.info("Uso de Memória em nível normal para {}.", device.getName());
            }
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

    private double getMemoryUsage(Exchange exchange, String containerName) throws Exception {
        log.info("GET Memory Usage");
        ProducerTemplate template = camelContext.createProducerTemplate();

        String prometheusQuery = String.format(
                "query=container_memory_usage_bytes{name=\"%s\"}", containerName
        );

        String response = template.requestBodyAndHeader(
                prometheusConfig.createURI(),
                null,
                Exchange.HTTP_QUERY,
                prometheusQuery,
                String.class
        );

        return parseMemoryUsageFromJson(response);
    }

    private double parseMemoryUsageFromJson(String jsonResponse) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        JsonNode resultNode = root.path("data").path("result").get(0);
        if (resultNode != null && resultNode.has("value")) {
            double memoryUsageBytes = resultNode.path("value").get(1).asDouble();
            double totalMemory = 1024 * 1024 * 1024;
            return memoryUsageBytes / totalMemory;
        } else {
            System.out.println("Métrica de Memória não encontrada para o container.");
            return 0.0;
        }
    }

    private void sendMqttCommand(Device device, String command) {
        String commandTopic = String.format("iot/sensor/control/%s",
                device.getIp().replace(".", "_"));

        camelContext.createProducerTemplate().sendBodyAndHeader(
                "paho:" + commandTopic,
                command,
                PahoConstants.MQTT_QOS, 1
        );

        log.info("Comando MQTT enviado para o dispositivo {} no tópico {}", device.getName(), commandTopic);
    }
}
