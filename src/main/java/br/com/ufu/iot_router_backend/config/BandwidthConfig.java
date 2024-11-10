package br.com.ufu.iot_router_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Configuration
@Getter
public class BandwidthConfig {

    @Value("${bandwidth.limit}")
    private int bandwidthLimit;

    @Value("${bandwidth.rate_interval}")
    private String rateInterval;
}
