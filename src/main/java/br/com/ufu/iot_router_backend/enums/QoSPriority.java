package br.com.ufu.iot_router_backend.enums;

import lombok.Getter;

@Getter
public enum QoSPriority {
	HIGH(100000, 50000, 0), 
	MEDIUM(50000, 25000, 1), 
	LOW(25000, 10000, 2);

	private final int maxRate;
	private final int minRate;
	private final int queueId;

	QoSPriority(int maxRate, int minRate, int queueId) {
		this.maxRate = maxRate;
		this.minRate = minRate;
		this.queueId = queueId;
	}
}
