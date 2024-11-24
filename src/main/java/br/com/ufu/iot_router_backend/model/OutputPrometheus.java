package br.com.ufu.iot_router_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class OutputPrometheus {

	@JsonProperty("status")
	private String status;
	
	@JsonProperty("data")
	private	DataMetric data;
	
}
