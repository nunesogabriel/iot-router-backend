package br.com.ufu.iot_router_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "prometheus")
public class MyPrometheusConfig {

    private String host;
    private String port;
    private String api;

    public String createURI() {
        return host +
                '/' +
                port +
                '/' +
                api;
    }
}
