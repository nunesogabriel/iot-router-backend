package br.com.ufu.iot_router_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
public class RyuConfig {
    @Value("${qos.bandwidth.default}")
    private int bandwidth;
    @Value("${qos.controller.url}")
    private String url;
}
