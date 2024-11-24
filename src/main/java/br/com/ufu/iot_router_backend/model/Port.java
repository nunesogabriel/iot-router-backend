package br.com.ufu.iot_router_backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Port {
	@JsonProperty("dpid")
	private String dpid;
	@JsonProperty("port_no")
	private String portNo;
	@JsonProperty("hw_addr")
	private String hwAddr;
	@JsonProperty("name")
	private String name;
}
