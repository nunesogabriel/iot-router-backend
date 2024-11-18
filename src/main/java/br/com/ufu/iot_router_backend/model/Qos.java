package br.com.ufu.iot_router_backend.model;

import br.com.ufu.iot_router_backend.enums.QoSDecision;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class Qos {
	private QoSDecision qosDecision;
	private int queueId;
}
