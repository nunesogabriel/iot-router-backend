package br.com.ufu.iot_router_backend.service;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ufu.iot_router_backend.config.MyPrometheusConfig;
import br.com.ufu.iot_router_backend.model.OutputPrometheus;

@Component
public class PrometheusClient {

    private static final Logger log = LoggerFactory.getLogger(PrometheusClient.class);
    @Autowired private  ObjectMapper objectMapper;
    @Autowired private MyPrometheusConfig prometheusConfig;

    /**
     * Faz uma consulta PromQL ao Prometheus e retorna o valor da métrica.
     *
     * @param query A consulta PromQL a ser executada.
     * @return O valor da métrica como um double.
     * @throws Exception Se houver erro na consulta ou no parsing.
     */
    public double queryMetric(String deviceIp, ProducerTemplate template) throws Exception {
        var q = String.format("query=probe_duration_seconds{instance=\"%s\"}", deviceIp);
        
        String response = template.requestBodyAndHeader(prometheusConfig.createURI(), null, Exchange.HTTP_QUERY,
        		q, String.class);

        if (response == null) {
            throw new RuntimeException("Erro na consulta ao Prometheus: " + response);
        }

        return parseMetricValue(response);
    }
    
    public OutputPrometheus getMetric(String query, ProducerTemplate template) throws JsonMappingException, JsonProcessingException {
    	String result = template
				.requestBodyAndHeader(prometheusConfig.createURI(), null, Exchange.HTTP_QUERY,
						query, String.class);
    	return new ObjectMapper().readValue(result, OutputPrometheus.class);
    }

    /**
     * Analisa o JSON retornado pelo Prometheus para extrair o valor da métrica.
     *
     * @param responseBody O corpo da resposta em formato JSON.
     * @return O valor da métrica como um double.
     * @throws Exception Se houver erro no parsing.
     */
    private double parseMetricValue(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode data = root.path("data").path("result");

        if (data.isArray() && data.size() > 0) {
            JsonNode valueNode = data.get(0).path("value");
            if (valueNode.isArray() && valueNode.size() > 1) {
                return valueNode.get(1).asDouble();
            }
        }

        log.warn("Nenhum valor encontrado na resposta do Prometheus: {}", responseBody);
        return 0.0; // Retorna 0.0 se nenhum valor for encontrado
    }
}
