package br.com.ufu.iot_router_backend.routes.orchestrator;

import org.apache.camel.builder.RouteBuilder;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class QosCheckRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("timer://checkQos?period=60000") // Verifica a cada 60 segundos, ajuste conforme necessário
                .process(exchange -> {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(new URI("http://ryu-controller:8080/qos/rules/0000000000000001"))
                            .GET()
                            .build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    int statusCode = response.statusCode();
                    String responseBody = response.body();

                    System.out.println("Status Code: " + statusCode);
                    System.out.println("Response Body: " + responseBody);
                }).log("${body}"); // Exibe o resultado da requisição no log
    }
}