package br.com.ufu.iot_router_backend.observabilidade;

import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class LatencyMetricsService {

    @Autowired
    private PrometheusMeterRegistry meterRegistry;

    public void recordLatency(double latency, String containerId) {
        meterRegistry.gauge("container_custom_latency_ms",
                Collections.singletonList(Tag.of("container_id", containerId)), latency);
    }

    public void incrementHighLatencyCount(String containerId) {
        meterRegistry.counter("high_latency_events", "container_id", containerId).increment();
    }

    public void incrementRedirectEvents(String containerId) {
        meterRegistry.counter("container_redirect_events", "container_id", containerId).increment();
    }

    public void incrementFailbackEvents(String containerId) {
        meterRegistry.counter("failback_events", "container_id", containerId).increment();
        System.out.println("Failback registrado para o container: " + containerId);
    }
}
