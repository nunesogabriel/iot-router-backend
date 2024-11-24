package br.com.ufu.iot_router_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Link {
	
	@JsonProperty("src")
	private Port src;
	
	@JsonProperty("dst")
	private Port dst;
}
