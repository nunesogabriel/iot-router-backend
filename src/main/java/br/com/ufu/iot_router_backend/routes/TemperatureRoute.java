package br.com.ufu.iot_router_backend.routes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class TemperatureRoute extends RouteBuilder {

    private final MeterRegistry meterRegistry;

    public TemperatureRoute(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void configure() throws Exception {

        from("paho:iot/sensor/temperature?brokerUrl=tcp://10.0.0.5:1883"
                + "&qos=2"                       // Nível de QoS 2 para garantir entrega
                + "&automaticReconnect=true"      // Habilita reconexão automática
                + "&keepAliveInterval=60"         // Intervalo de keep-alive em segundos
        )
                .routeId("MQTT_IOT")
                .throttle(1000)
                .process(exchange -> {
                    String payload = exchange.getIn().getBody(String.class);
                    JsonObject temperatureJson= JsonParser.parseString(payload).getAsJsonObject();
                    long temperature = temperatureJson.get("temperature").getAsLong();
                });
    }
}
