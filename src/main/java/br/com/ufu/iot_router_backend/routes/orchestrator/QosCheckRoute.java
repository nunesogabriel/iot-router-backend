package br.com.ufu.iot_router_backend.routes.orchestrator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class QosCheckRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("timer://checkQos?period=60000") // Verifica a cada 60 segundos, ajuste conforme necessário
                .process(exchange -> {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(new URI("http://10.0.0.220:8080/qos/rules/0000000000000001"))
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    int statusCode = response.statusCode();
                    String responseBody = response.body();
                }); // Exibe o resultado da requisição no log
    }
}