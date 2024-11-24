package br.com.ufu.iot_router_backend.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class TemperatureRoute extends RouteBuilder {
	
	@Override
	public void configure() throws Exception {

		from("paho:iot/sensor/temperature?brokerUrl=tcp://10.0.0.237:1883&qos=1&maxInflight=50")
			.routeId("MQTT_IOT")
			.threads(10)
				.process(exchange -> {
					String payload = exchange.getIn().getBody(String.class);
					JsonObject temperatureJson = JsonParser.parseString(payload).getAsJsonObject();
					long temperature = temperatureJson.get("temperature").getAsLong();
				});
	}
}
