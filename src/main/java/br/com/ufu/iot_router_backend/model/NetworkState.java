package br.com.ufu.iot_router_backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class NetworkState {
    private long previousReceiveBytes;
    private long previousTransmitBytes;
    private long previousTimestamp;
}

