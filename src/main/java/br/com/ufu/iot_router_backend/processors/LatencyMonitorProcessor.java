package br.com.ufu.iot_router_backend.processors;

import com.google.gson.Gson;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Iterator;


@Component
public class LatencyMonitorProcessor implements Processor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LatencyMonitorProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        String prometheusResponse = exchange.getIn().getBody(String.class);
        LOGGER.info("Prometheus Response ....");
        var jsonObject = new JSONObject(prometheusResponse);
//        printAllKeys(jsonObject, "");
        extractLatency(jsonObject);
        processSimulatedDevicesLatency(jsonObject);
        exchange.getIn().setBody(extractLatency(jsonObject, "mn.mqtt"));
    }

    public void processSimulatedDevicesLatency(JSONObject jsonObject) {
        // Navegar até a chave "data"
        JSONObject data = jsonObject.optJSONObject("data");
        if (data == null) {
            System.out.println("Data key not found");
            return;
        }

        // Navegar até a chave "result" (que é um JSONArray)
        JSONArray resultsArray = data.optJSONArray("result");
        if (resultsArray == null) {
            System.out.println("Results array not found");
            return;
        }

        // Percorrer os itens no JSONArray "result"
        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultItem = resultsArray.optJSONObject(i);
            if (resultItem != null) {
                // Navegar até a chave "metric"
                JSONObject metric = resultItem.optJSONObject("metric");
                if (metric != null) {
                    // Obter o nome do container
                    String containerName = metric.optString("name");

                    // Verificar se o container é um dispositivo simulado (mn.mqtt1, mn.mqtt2, etc.)
                    if (containerName.startsWith("mn.mqtt")) {
                        // Navegar até a chave "value" para pegar a latência (que é um JSONArray)
                        JSONArray valueArray = resultItem.optJSONArray("value");
                        if (valueArray != null && valueArray.length() >= 2) {
                            double timestamp = valueArray.optDouble(0);  // O primeiro valor é o timestamp
                            double latency = valueArray.optDouble(1);    // O segundo valor é a latência
                            System.out.println("Container: " + containerName + " | Timestamp: " + timestamp + ", Latency: " + latency);

                            // Aqui você define sua lógica de ação com base na latência
                            takeActionBasedOnLatency(containerName, latency);
                        }
                    }
                }
            }
        }
    }

    private void takeActionBasedOnLatency(String containerName, double latency) {
        // Defina suas ações de acordo com os valores de latência, por exemplo:
        double threshold = 1.5; // Defina o limite de latência relevante para sua aplicação

        if (latency > threshold) {
            System.out.println("High latency detected for " + containerName + ". Taking action...");
            // Ação para latência alta, por exemplo, ajustar alocação de recursos
            adjustResourcesForContainer(containerName);
        } else {
            System.out.println("Latency for " + containerName + " is within normal limits.");
        }
    }

    private void adjustResourcesForContainer(String containerName) {
        // Implemente a lógica de ajuste de recursos ou outra ação aqui
        System.out.println("Adjusting resources for container: " + containerName);
    }


    public Double extractLatency(JSONObject jsonObject, String targetContainerName) {
        // Navegar até a chave "data"
        JSONObject data = jsonObject.optJSONObject("data");
        if (data == null) {
            System.out.println("Data key not found");
            return 0.0D;
        }

        // Navegar até a chave "result" (que é um JSONArray)
        JSONArray resultsArray = data.optJSONArray("result");
        if (resultsArray == null) {
            System.out.println("Results array not found");
            return 0.0D;
        }

        // Percorrer os itens no JSONArray "result"
        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultItem = resultsArray.optJSONObject(i);
            if (resultItem != null) {
                // Navegar até a chave "metric"
                JSONObject metric = resultItem.optJSONObject("metric");
                if (metric != null) {
                    // Obter o nome do container
                    String containerName = metric.optString("name");

                    // Comparar com o container desejado
                    if (containerName.contains(targetContainerName)) {
                        // Navegar até a chave "value" para pegar a latência (que é um JSONArray)
                        JSONArray valueArray = resultItem.optJSONArray("value");
                        if (valueArray != null && valueArray.length() >= 2) {
                            double timestamp = valueArray.optDouble(0);  // O primeiro valor é o timestamp
                            double latency = valueArray.optDouble(1);    // O segundo valor é a latência
                            System.out.println("Timestamp: " + timestamp + ", Latency: " + latency);
                            return latency;
                        } else {
                            System.out.println("Latency value not found for container: " + containerName);
                        }
                        return 0.0D;  // Saímos após encontrar o container
                    }
                }
            }
        }
        return 0.0D;
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

    public void extractLatency(JSONObject jsonObject) {
        // Navegar até a chave "data"
        JSONObject data = jsonObject.optJSONObject("data");
        if (data == null) {
            System.out.println("Data key not found");
            return;
        }

        // Navegar até a chave "result" (que é um JSONArray)
        JSONArray resultsArray = data.optJSONArray("result");
        if (resultsArray == null) {
            System.out.println("Results array not found");
            return;
        }

        // Percorrer os itens no JSONArray "result"
        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject resultItem = resultsArray.optJSONObject(i);
            if (resultItem != null) {
                // Navegar até a chave "metric"
                JSONObject metric = resultItem.optJSONObject("metric");
                if (metric != null) {
                    // Obter o nome do container ou outra métrica relevante
                    String containerName = metric.optString("name");
                    System.out.println("Container: " + containerName);
                }

                // Navegar até a chave "value" para pegar a latência (que é um JSONArray)
                JSONArray valueArray = resultItem.optJSONArray("value");
                if (valueArray != null && valueArray.length() >= 2) {
                    double timestamp = valueArray.optDouble(0);  // O primeiro valor é o timestamp
                    double latency = valueArray.optDouble(1);    // O segundo valor é a latência
                    System.out.println("Timestamp: " + timestamp + ", Latency: " + latency);
                }
            }
        }
    }


}