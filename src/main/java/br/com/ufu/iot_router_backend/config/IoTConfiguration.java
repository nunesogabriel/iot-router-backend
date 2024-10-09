package br.com.ufu.iot_router_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "iot")  // Usamos o prefixo "iot" para referenciar o início do YAML
public class IoTConfiguration {

    private List<ResourcePolicy> policies;

    // Getters e setters
    public List<ResourcePolicy> getPolicies() {
        return policies;
    }

    public void setPolicies(List<ResourcePolicy> policies) {
        this.policies = policies;
    }

    // Classe interna para definir a estrutura dos recursos (dispositivos)
    public static class ResourcePolicy {
        private String id;
        private String type;
        private int qos;
        private String priority;
        private String bandwidth;
        private long latencyThreshold;

        // Getters e setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getQoS() {
            return qos;
        }

        public void setQoS(int qos) {
            this.qos = qos;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }

        public String getBandwidth() {
            return bandwidth;
        }

        public void setBandwidth(String bandwidth) {
            this.bandwidth = bandwidth;
        }

        public long getLatencyThreshold() {
            return latencyThreshold;
        }

        public void setLatencyThreshold(long latencyThreshold) {
            this.latencyThreshold = latencyThreshold;
        }
    }
}
