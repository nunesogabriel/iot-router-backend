package br.com.ufu.iot_router_backend.processors.prometheus;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class GetIpsContainersProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://ip_fetcher:5000/get_ips"))  // URL da API REST do Ryu
                .header("Content-Type", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        exchange.getIn().setBody(response.body());
    }
}
