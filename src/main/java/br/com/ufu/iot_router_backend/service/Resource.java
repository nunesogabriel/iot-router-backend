package br.com.ufu.iot_router_backend.service;

public interface Resource {
    String getId();
    String getType();
    int getQoS();  // Nível de QoS (Qualidade de Serviço)
    int getPriority();
    long measureLatency();  // Medir latência do dispositivo
    void setBandwidth(int bandwidth);  // Ajustar largura de banda
    int getBandwidth();  // Obter largura de banda atual
    long getLatencyThreshold();  // Limite de latência configurado
}
