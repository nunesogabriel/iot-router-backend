package br.com.ufu.iot_router_backend.routes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class BandwidthRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("paho:iot/sensor/bandwidth?brokerUrl=tcp://10.0.0.5:1883"
                + "&qos=2"                         // Nível de QoS 2 para garantir entrega
                + "&automaticReconnect=true"      // Habilita reconexão automática
                + "&keepAliveInterval=60")
                .process(exchange -> {
                    String payload = exchange.getIn().getBody(String.class);
                    JsonObject metrics = JsonParser.parseString(payload).getAsJsonObject();
                    var bandwidth = metrics.get("bandwidth").getAsDouble();
                    // Aqui você cria a mensagem de resposta dependendo da sua lógica
                    JsonObject response = new JsonObject();
                    if (bandwidth > 80) {
                        response.addProperty("message", "High bandwidth usage detected!");
                    } else {
                        response.addProperty("message", "Bandwidth usage is normal.");
                    }
                    exchange.getIn().setBody(response.toString());

                }).to("paho:iot/sensor/response?brokerUrl=tcp://10.0.0.5:1883"
                        + "&qos=2"                         // Nível de QoS 2
                        + "&automaticReconnect=true"      // Habilita reconexão automática
                        + "&keepAliveInterval=60");
    }
}
