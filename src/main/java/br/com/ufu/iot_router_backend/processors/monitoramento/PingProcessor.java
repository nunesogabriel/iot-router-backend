package br.com.ufu.iot_router_backend.processors.monitoramento;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PingProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        log.info("Monitoramento containers Devices Simulados");
        List<String> containerNames =
                List.of("mn.mqtt1", "mn.mqtt2",
                        "mn.mqtt3", "mn.mqtt4",
                        "mn.mqtt5", "mn.mqtt6");

        execute(containerNames);
    }

    private void execute(List<String> containerNames) throws Exception {
        for (String deviceIp : containerNames) {
            double latency = getLatency(deviceIp);
            System.out.println("Latência do dispositivo " + deviceIp + ": " + latency + " ms");

            // Verifica se a latência ultrapassa o limite de 250ms
            if (latency > 200) {
                // Combina diferentes ações para reduzir a latência
                reduceDataFrequency(deviceIp);
                adjustBandwidth(deviceIp, 100);  // Exemplo: ajustar a largura de banda para 100Mbps
                prioritizeCriticalTraffic(deviceIp);
                balanceNetworkLoad(deviceIp);
            } else {
                System.out.println("Latência dentro dos limites aceitáveis.");
            }
        }
    }

    // Método para obter a latência via ping
    private double getLatency(String deviceIp) throws Exception {
        String command = "ping -c 10 " + deviceIp;
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        process.waitFor();

        return extractAverageLatency(output.toString());
    }

    private double extractAverageLatency(String pingOutput) {
        Pattern pattern = Pattern.compile("rtt min/avg/max/mdev = (\\d+\\.\\d+)/(\\d+\\.\\d+)/(\\d+\\.\\d+)/(\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(pingOutput);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(2));  // Latência média (segundo valor)
        }

        return 0.0;  // Caso não consiga extrair a latência
    }

    // Ação 1: Reduzir a frequência de envio de dados
    private void reduceDataFrequency(String deviceIp) {
        // Lógica para reduzir a frequência de envio dos dados
        System.out.println("Reduzindo frequência de envio de dados para o dispositivo: " + deviceIp);
        // Aqui você pode enviar um comando ao dispositivo via MQTT, HTTP, etc.
    }

    // Ação 2: Ajustar a largura de banda usando SDN
    private void adjustBandwidth(String deviceIp, int bandwidth) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String jsonBody = "{\"port_name\":\"s1-eth1\", \"type\": \"linux-htb\", \"max-rate\": \"500000\", \"queues\":[{\"max_rate\": \"200000\", \"min_rate\": \"100000\"}]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://ryu-controller:8080/qos/queue/0000000000000001"))  // URL da API REST do Ryu
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            System.out.println("Largura de banda ajustada com sucesso para " + deviceIp + ": " + bandwidth + " Mbps");
        } else {
            System.out.println("Falha ao ajustar largura de banda: " + response.body());
        }
    }

    // Ação 3: Priorizar tráfego crítico
    private void prioritizeCriticalTraffic(String deviceIp) {
        // Lógica para priorizar pacotes críticos
        System.out.println("Priorizando tráfego crítico para o dispositivo: " + deviceIp);
        // Implementar regra de priorização no controlador SDN ou na aplicação
    }

    // Ação 4: Redistribuir carga de rede
    private void balanceNetworkLoad(String deviceIp) {
        // Lógica para balancear a carga de rede entre os switches/dispositivos
        System.out.println("Balanceando a carga de rede para o dispositivo: " + deviceIp);
        // Implementar lógica de reatribuição de rotas ou switches
    }
}
