package br.com.ufu.iot_router_backend.routes;

import br.com.ufu.iot_router_backend.processors.LatencyProcessor;
import br.com.ufu.iot_router_backend.strategy.GroupedBodyAggregationStrategy;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MQTTRoute extends RouteBuilder {

    private final MeterRegistry meterRegistry;

    public MQTTRoute(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void configure() throws Exception {

        from("paho:iot/sensor/temperature?brokerUrl=tcp://mosquitto:1883")
                .routeId("MQTT_IOT")
                .throttle(1000)
                .choice()
                    .when(simple("${body} < 15 || ${body} > 30"))
                        .process(new LatencyProcessor(meterRegistry))
                        .log("Temperatura critica ${body}")
                        .setHeader("CamelPahoQos", constant(2))
                        .to("paho:iot/sensor/response?brokerUrl=tcp://mosquitto:1883")
                    .otherwise()
                        .log("Temperatura normal ${body}.")
                        .aggregate(constant(true), new GroupedBodyAggregationStrategy())
                        .completionSize(10)  // Agrupar 10 mensagens por lote
                        .completionTimeout(60000)  // Ou enviar após 1 minuto
                        .setHeader("CamelPahoQos", constant(0))  // Usar QoS 0 para mensagens normais
                        .log("Enviando lote de mensagens normais: ${body}")
                        .to("paho:iot/sensor/batch?brokerUrl=tcp://mosquitto:1883");
    }
}
