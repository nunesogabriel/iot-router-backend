package br.com.ufu.iot_router_backend.processors;

import com.google.gson.Gson;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class BalanceMonitorProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceMonitorProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        var responsePrometheus = exchange.getIn().getBody(String.class);
        exchange.getIn().setBody(extractCpuUsage(responsePrometheus));
    }

    public void printAllKeys(JSONObject jsonObj, String prefix) {
        Iterator<String> keys = jsonObj.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            System.out.println("key = " + key);
            Object value = jsonObj.get(key);

            if (value instanceof JSONObject) {
                System.out.println(prefix + "JSONObject: " + key);
                printAllKeys((JSONObject) value, prefix + "    ");
            } else if (value instanceof JSONArray) {
                System.out.println(prefix + "JSONArray: " + key);
                JSONArray array = (JSONArray) value;
                for (int i = 0; i < array.length(); i++) {
                    Object arrayItem = array.get(i);
                    if (arrayItem instanceof JSONObject) {
                        printAllKeys((JSONObject) arrayItem, prefix + "    ");
                    }
                }
            } else {
                System.out.println(prefix + "Key: " + key + " -> Value: " + value);
            }
        }
    }

    public double extractCpuUsage(String jsonResponse) throws Exception {
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
        String cpuUsageString = valueArray.getString(1);

        LOGGER.info("CPU = {}", cpuUsageString);
        return Double.parseDouble(cpuUsageString);
    }
}
