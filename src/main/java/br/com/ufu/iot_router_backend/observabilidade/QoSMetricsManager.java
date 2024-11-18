package br.com.ufu.iot_router_backend.observabilidade;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.ufu.iot_router_backend.service.RyuService;
import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheus.PrometheusMeterRegistry;

@Component
public class QoSMetricsManager {

    @Autowired
    private RyuService ryuService;

    @Autowired
    private PrometheusMeterRegistry meterRegistry;

    // Atualiza métricas das portas
    public void updatePortMetrics(String dpid) {
        List<Map<String, Object>> ports = ryuService.getSwitchPorts(dpid);

        for (Map<String, Object> portData : ports) {
            String portNo = String.valueOf(portData.get("port_no"));
            double txBytes = Double.parseDouble(String.valueOf(portData.getOrDefault("tx_bytes", "0")));
            double rxBytes = Double.parseDouble(String.valueOf(portData.getOrDefault("rx_bytes", "0")));
            double txPackets = Double.parseDouble(String.valueOf(portData.getOrDefault("tx_packets", "0")));
            double rxPackets = Double.parseDouble(String.valueOf(portData.getOrDefault("rx_packets", "0")));

            // Atualizando métricas
            meterRegistry.gauge("port_traffic_bytes", 
                List.of(Tag.of("switch_id", dpid), Tag.of("port_no", portNo)), 
                txBytes + rxBytes);
            
            meterRegistry.gauge("switch_port_packets_total", 
                List.of(Tag.of("switch_id", dpid), Tag.of("port_no", portNo)), 
                txPackets + rxPackets);
        }
    }

    // Atualiza métricas das filas
    public void updateQueueMetrics(String dpid) {
        List<Map<String, Object>> queues = ryuService.getQueueStats(dpid);
        for (Map<String, Object> queue : queues) {
            String queueId = String.valueOf(queue.get("queue_id"));
            double txPackets = Double.parseDouble(String.valueOf(queue.getOrDefault("tx_packets", "0")));

            meterRegistry.gauge("queue_stats_packets", 
                List.of(Tag.of("switch_id", dpid), Tag.of("queue_id", queueId)), 
                txPackets);
        }
    }

    // Atualiza métricas dos fluxos
    public void updateFlowMetrics(String dpid) {
        List<Map<String, Object>> flows = ryuService.getFlowStats(dpid);
        for (Map<String, Object> flow : flows) {
            String flowId = String.valueOf(flow.getOrDefault("cookie", "0"));
            double packetCount = Double.parseDouble(String.valueOf(flow.getOrDefault("packet_count", "0")));

            meterRegistry.gauge("flow_stats_packets", 
                List.of(Tag.of("switch_id", dpid), Tag.of("flow_id", flowId)), 
                packetCount);
        }
    }

    // Atualiza todas as métricas de um switch
    private void updateMetricsForSwitch(String dpid) {
        this.updatePortMetrics(dpid);
        this.updateQueueMetrics(dpid);
        this.updateFlowMetrics(dpid);
    }

    // Atualiza todas as métricas de todos os switches
    public void updateMetricsForAllSwitches() {
        var dpids = this.ryuService.getAllSwitches();
        for (String dpid : dpids) {
            updateMetricsForSwitch(dpid);
        }
    }
}