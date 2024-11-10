package br.com.ufu.iot_router_backend.routes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class LatencyRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
    	errorHandler(defaultErrorHandler()
                .maximumRedeliveries(-1) // -1 significa tentativas infinitas
                .redeliveryDelay(5000)   // atraso de 5 segundos entre as tentativas
            );
        from("paho:iot/sensor/latency?brokerUrl=tcp://10.0.0.237:1883"
                + "&qos=2"                         // Nível de QoS 2 para garantir entrega
                + "&automaticReconnect=true"      // Habilita reconexão automática
                + "&keepAliveInterval=60")
                .process(exchange -> {
                    String payload = exchange.getIn().getBody(String.class);
                    JsonObject metrics = JsonParser.parseString(payload).getAsJsonObject();
                    var latency = metrics.get("latency").getAsDouble();
                    // Aqui você cria a mensagem de resposta dependendo da sua lógica
                    JsonObject response = new JsonObject();
                    if (latency > 80) {
                        response.addProperty("message", "High Latency usage detected!");
                    } else {
                        response.addProperty("message", "Latency usage is normal.");
                    }
                    exchange.getIn().setBody(response.toString());

                }).to("paho:iot/sensor/response?brokerUrl=tcp://10.0.0.237:1883"
                        + "&qos=2"                         // Nível de QoS 2
                        + "&automaticReconnect=true"      // Habilita reconexão automática
                        + "&keepAliveInterval=60");
    }
}

