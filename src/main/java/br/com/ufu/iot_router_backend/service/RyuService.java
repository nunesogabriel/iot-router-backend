package br.com.ufu.iot_router_backend.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import br.com.ufu.iot_router_backend.config.RyuConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RyuService {

	@Autowired
	private RyuConfig ryuConfig;

	@Autowired
	private RestTemplate restTemplate;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<String> getAllSwitches() {
		String url = ryuConfig.getUrl() + "/v1.0/topology/switches";
		RestTemplate restTemplate = new RestTemplate();
		try {
			ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);
			return (List<String>) response.getBody().stream()
					.map(switchObj -> ((Map<String, Object>) switchObj).get("dpid").toString())
					.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Erro ao obter switches do Ryu.", e);
			return Collections.emptyList();
		}
	}

	public List<Map<String, Object>> getSwitchPorts(String dpid) {
		var dpidNumeric = Integer.parseInt(dpid);
		String apiUrl = ryuConfig.getUrl() + "/stats/port/" + String.valueOf(dpidNumeric);
		try {
			
	        ResponseEntity<Map<String, List<Map<String, Object>>>> response = restTemplate.exchange(
	            apiUrl,
	            HttpMethod.GET,
	            null,
	            new ParameterizedTypeReference<>() {}
	        );

	        Map<String, List<Map<String, Object>>> data = response.getBody();

	        if (data != null && data.containsKey(String.valueOf(dpidNumeric))) {
	            return data.get(String.valueOf(dpidNumeric));
	        } else {
	            System.err.println("Nenhuma porta encontrada para o switch " + dpid);
	            return List.of();
	        }
	    } catch (Exception e) {
	        System.err.println("Erro ao consultar portas do switch " + dpid + ": " + e.getMessage());
	        return List.of();
	    }
	}

	public List<Map<String, Object>> getQueueStats(String dpid) {
		var dpidAux = String.valueOf(Integer.parseInt(dpid));
		String apiUrl = ryuConfig.getUrl() + "/stats/queue/" + dpidAux;
		try {
			 ResponseEntity<Map<String, List<Map<String, Object>>>> response = restTemplate.exchange(
			            apiUrl,
			            HttpMethod.GET,
			            null,
			            new ParameterizedTypeReference<>() {}
			        );
			 Map<String, List<Map<String, Object>>> data = response.getBody();

		        if (data != null && data.containsKey(String.valueOf(dpidAux))) {
		            return data.get(String.valueOf(dpidAux));
		        } else {
		            System.err.println("Nenhuma porta encontrada para o switch " + dpid);
		            return List.of();
		        }
		} catch (Exception e) {
			System.err.println("Erro ao consultar filas do switch " + dpid + ": " + e.getMessage());
			return List.of();
		}
	}

	public List<Map<String, Object>> getFlowStats(String dpid) {
		var dpidAux = String.valueOf(Integer.parseInt(dpid));
		String apiUrl = ryuConfig.getUrl() + "/stats/flow/" + dpidAux;
		try {
			 ResponseEntity<Map<String, List<Map<String, Object>>>> response = restTemplate.exchange(
			            apiUrl,
			            HttpMethod.GET,
			            null,
			            new ParameterizedTypeReference<>() {}
			        );
			 Map<String, List<Map<String, Object>>> data = response.getBody();

		        if (data != null && data.containsKey(String.valueOf(dpidAux))) {
		            return data.get(String.valueOf(dpidAux));
		        } else {
		            System.err.println("Nenhuma porta encontrada para o switch " + dpid);
		            return List.of();
		        }
		} catch (Exception e) {
			System.err.println("Erro ao consultar filas do switch " + dpid + ": " + e.getMessage());
			return List.of();
		}
	}
}
