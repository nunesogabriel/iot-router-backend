package br.com.ufu.iot_router_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ResultMetric {
	
	@JsonProperty("metric")
	private Metric metric;
	
	@JsonProperty("value")
	private String[] value;

}
