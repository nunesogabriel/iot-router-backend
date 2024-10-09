package br.com.ufu.iot_router_backend.service;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.ArrayList;
import java.util.List;

public class NetworkOrchestrator {
    private List<Resource> resources = new ArrayList<>();
    private final MeterRegistry meterRegistry;

    public NetworkOrchestrator(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void registerResource(Resource resource) {
        resources.add(resource);
    }

    // Ajuste dinâmico de largura de banda e latência com base em métricas
    public void orchestrateNetwork() {
        for (Resource resource : resources) {
            // Verificar se a latência está dentro do limite configurado
            if (resource.measureLatency() > resource.getLatencyThreshold()) {
                System.out.println("Latência alta detectada! Ajustando rede para " + resource.getId());
                // Redirecionar ou alocar mais largura de banda para este dispositivo
                resource.setBandwidth(resource.getBandwidth() + 2);  // Exemplo de ajuste de banda
            }

            // Ajustar largura de banda para dispositivos com QoS superior
            if (resource.getQoS() == 2) {
                System.out.println("Alocando mais largura de banda para dispositivo crítico: " + resource.getId());
                resource.setBandwidth(resource.getBandwidth() + 5);  // Ajustar banda
            }
        }
    }
}
