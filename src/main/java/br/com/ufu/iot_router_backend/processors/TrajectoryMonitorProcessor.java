package br.com.ufu.iot_router_backend.processors;

import com.google.gson.Gson;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class TrajectoryMonitorProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrajectoryMonitorProcessor.class);


    @Override
    public void process(Exchange exchange) throws Exception {
        String prometheusResponse = exchange.getIn().getBody(String.class);
        exchange.getIn().setBody(extractPacketErrors(prometheusResponse));
    }

    public double extractPacketErrors(String jsonResponse) throws Exception {
        JSONObject jsonObject = new JSONObject(jsonResponse);

        if (!"success".equals(jsonObject.getString("status"))) {
            LOGGER.info("Falha na coleta de métricas do Prometheus");
        }

        JSONArray results = jsonObject.getJSONObject("data").getJSONArray("result");

        if (results.length() == 0) {
            LOGGER.info("Nenhum resultado encontrado na resposta do Prometheus");
        }

        JSONObject result = results.getJSONObject(0);
        JSONArray valueArray = result.getJSONArray("value");
        String packetErrorsString = valueArray.getString(1);

        LOGGER.info("packetErrorsString = {}", packetErrorsString);

        return Double.parseDouble(packetErrorsString);
    }
}
