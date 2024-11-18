package br.com.ufu.iot_router_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Host {
	
	@JsonProperty("mac")
	private String mac;
	@JsonProperty("ipv4")
	private String[] ipv4;
	@JsonProperty("ipv6")
	private String[] ipv6;
	@JsonProperty("port")
	private Port port;
}
