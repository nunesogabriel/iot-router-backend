package br.com.ufu.iot_router_backend.observabilidade;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheus.PrometheusMeterRegistry;

@Component
public class MetricsManager {

	@Autowired
	private PrometheusMeterRegistry meterRegistry;

	private final AtomicInteger qosApplicationsCount;

	public MetricsManager(PrometheusMeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.qosApplicationsCount = new AtomicInteger(0);
		meterRegistry.gauge("qos_applications_total", Collections.singletonList(Tag.of("type", "active")),
				qosApplicationsCount);
	}

	public void incrementRyuIterations() {
		meterRegistry.counter("ryu_iterations_total").increment();
	}

	public void observeLatency(double seconds, String containerId) {
		meterRegistry.timer("device_latency_seconds", Collections.singletonList(Tag.of("container_id", containerId)))
				.record((long) (seconds * 1_000_000_000), java.util.concurrent.TimeUnit.NANOSECONDS);
	}

	public void updateQoSApplications(int activeQoS) {
		qosApplicationsCount.set(activeQoS);
	}

	public void incrementHighLatencyCount(String containerId) {
		meterRegistry.counter("high_latency_events", "container_id", containerId).increment();
	}

	public void incrementCreateQueue(String containerId) {
		meterRegistry.counter("qos_create_queue", "container_id", containerId).increment();
	}

	public void incrementAvailableDevice(String containerId) {
		meterRegistry.counter("devices_status", "container_id", containerId).increment();
	}

	public void incrementDownDevice(String containerId) {
		meterRegistry.counter("devices_down", "container_id", containerId).increment();
	}

	public void incrementApplyRule(String containerId) {
		meterRegistry.counter("qos_apply_rule", "container_id", containerId).increment();
	}

	public void incrementRedirectTraffic(String containerId) {
		meterRegistry.counter("qos_redirect_traffic", "container_id", containerId).increment();
	}

	public void incrementRedirectEvents(String containerId) {
		meterRegistry.counter("container_redirect_events", "container_id", containerId).increment();
	}

	public void incrementSendCommandDevice(String containerId) {
		meterRegistry.counter("command_device", "container_id", containerId).increment();
	}

	public void incrementBroker(String containerId) {
		meterRegistry.counter("broker_latency", "container_id", containerId).increment();
	}

	public void incrementFailbackEvents(String containerId) {
		meterRegistry.counter("failback_events", "container_id", containerId).increment();
		System.out.println("Failback registrado para o container: " + containerId);
	}
}
