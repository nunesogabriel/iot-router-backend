package br.com.ufu.iot_router_backend.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class DataMetric {
	
	@JsonProperty("resultType")
	private String resultType;

	@JsonProperty("result")
	private List<ResultMetric> resultMetric;
}
