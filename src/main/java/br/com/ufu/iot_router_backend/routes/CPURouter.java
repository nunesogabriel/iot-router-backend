package br.com.ufu.iot_router_backend.routes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class CPURouter extends RouteBuilder {

    private final MeterRegistry meterRegistry;

    private CPURouter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void configure() throws Exception {
        from("paho:iot/sensor/cpu?brokerUrl=tcp://10.0.0.5:1883"
                + "&qos=2"                         // Nível de QoS 2 para garantir entrega
                + "&automaticReconnect=true"      // Habilita reconexão automática
                + "&keepAliveInterval=60")
                .process(exchange -> {
                    String payload = exchange.getIn().getBody(String.class);
                    JsonObject metrics = JsonParser.parseString(payload).getAsJsonObject();
                    var cpuUsage = metrics.get("cpu_usage").getAsDouble();
                    // Aqui você cria a mensagem de resposta dependendo da sua lógica
                    JsonObject response = new JsonObject();
                    if (cpuUsage > 80) {
                        response.addProperty("message", "High CPU usage detected!");
                    } else {
                        response.addProperty("message", "CPU usage is normal.");
                    }
                    exchange.getIn().setBody(response.toString());
                })
                // Envia a resposta ao tópico de resposta
                .to("paho:iot/sensor/response?brokerUrl=tcp://10.0.0.5:1883"
                        + "&qos=2"                         // Nível de QoS 2
                        + "&automaticReconnect=true"      // Habilita reconexão automática
                        + "&keepAliveInterval=60");
    }
}