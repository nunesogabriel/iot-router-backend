package br.com.ufu.iot_router_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Metric {
	@JsonProperty("id")
	private String id;
	@JsonProperty("image")
	private String image;
	@JsonProperty("instance")
	private String instance;
	@JsonProperty("interface")
	private String interfac;
	@JsonProperty("job")
	private String job;
	@JsonProperty("name")
	private String name;
}
