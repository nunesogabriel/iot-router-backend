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
//        LOGGER.info(new Gson().toJson(new JSONObject(responsePrometheus)));
//        printAllKeys(new JSONObject(responsePrometheus), "");
        exchange.getIn().setBody(extractCpuUsage(responsePrometheus));
    }

    public void printAllKeys(JSONObject jsonObj, String prefix) {
        Iterator<String> keys = jsonObj.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            System.out.println("key = " + key);
            Object value = jsonObj.get(key);

            if (value instanceof JSONObject) {
                // Se o valor for um JSONObject, percorre ele recursivamente
                System.out.println(prefix + "JSONObject: " + key);
                printAllKeys((JSONObject) value, prefix + "    ");
            } else if (value instanceof JSONArray) {
                // Se o valor for um JSONArray, percorre os elementos
                System.out.println(prefix + "JSONArray: " + key);
                JSONArray array = (JSONArray) value;
                for (int i = 0; i < array.length(); i++) {
                    Object arrayItem = array.get(i);
                    if (arrayItem instanceof JSONObject) {
                        printAllKeys((JSONObject) arrayItem, prefix + "    ");
                    }
                }
            } else {
                // Se o valor for um valor simples (String, Number, etc)
                System.out.println(prefix + "Key: " + key + " -> Value: " + value);
            }
        }
    }

    public double extractCpuUsage(String jsonResponse) throws Exception {
        // Parse o JSON de resposta do Prometheus
        JSONObject jsonObject = new JSONObject(jsonResponse);

        // Verifica se o status da resposta é "success"
        if (!"success".equals(jsonObject.getString("status"))) {
            LOGGER.info("Falha na coleta de métricas do Prometheus");
        }

        // Navega até o campo "result" no JSON
        JSONArray results = jsonObject.getJSONObject("data").getJSONArray("result");

        // Verifica se há resultados
        if (results.length() == 0) {
            LOGGER.info("Nenhum resultado encontrado na resposta do Prometheus");
        }

        // Extrai o primeiro valor de uso de CPU (assumimos que há apenas um resultado)
        JSONObject result = results.getJSONObject(0);
        JSONArray valueArray = result.getJSONArray("value");

        // O segundo elemento do array "value" é o uso de CPU em segundos (como string)
        String cpuUsageString = valueArray.getString(1);

        LOGGER.info("CPU = {}", cpuUsageString);
        // Converte a string para double (segundos de CPU usados)
        return Double.parseDouble(cpuUsageString);
    }
}
