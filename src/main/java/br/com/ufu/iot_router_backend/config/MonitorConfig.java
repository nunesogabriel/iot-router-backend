package br.com.ufu.iot_router_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "monitor")
public class MonitorConfig {
    private int bandwidthLimit;
    private double cpuCritialThreshold;
    private double cpuAlertThreshold;
    private int latencyThreshold;
    private double memoryThresholdCritical;
    private double memoryThresholdAlert;
    private int latencyPing;
}
