package br.com.ufu.iot_router_backend.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class LatencyProcessor implements Processor {

    private final MeterRegistry meterRegistry;

    public LatencyProcessor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        // Inicia o Timer para medir a latência
        Timer.Sample sample = Timer.start(meterRegistry);

        // Simular processamento da mensagem
        try {
            // Aqui seria o processamento real
            Thread.sleep(100);  // Exemplo de tempo de processamento
            meterRegistry.counter("camel.request.count").increment();
        } finally {
            // Registra o tempo de execução no Micrometer Timer
            sample.stop(Timer.builder("camel.request.latency")
                    .description("Latência das requisições no Camel")
                    .register(meterRegistry));
        }
    }
}
