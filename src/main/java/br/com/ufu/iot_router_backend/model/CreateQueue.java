package br.com.ufu.iot_router_backend.model;

import lombok.Data;

@Data
public class CreateQueue {
	
	private String switchId;
	private String portName;
	private double maxRate;
	private double minRate; 
	private int queueId;
}
