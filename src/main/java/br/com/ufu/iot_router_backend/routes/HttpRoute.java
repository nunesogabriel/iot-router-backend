package br.com.ufu.iot_router_backend.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class HttpRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        restConfiguration()
                .component("servlet")  // Use o componente servlet para lidar com HTTP
                .contextPath("/sensor")  // Define o contexto base (opcional)
                .port(8080)  // Porta do servidor (opcional, apenas para especificar se for diferente)
                .dataFormatProperty("prettyPrint", "true");  // Habilita o pretty print para JSON (opcional)

        rest("/sensor")
                .post("/data")
                .consumes("application/json")
                .produces("application/json")

                .to("direct:processSensorData");

        // Processa a requisição
        from("direct:processSensorData")
                .log("Recebendo dados do sensor: ${body}")  // Log do corpo da requisição
                .process(exchange -> {
                    // Lógica de processamento do sensor
                });
    }

}
