//package br.com.ufu.iot_router_backend.service;
//
//import java.util.Queue;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//import org.eclipse.paho.client.mqttv3.IMqttClient;
//import org.eclipse.paho.client.mqttv3.MqttClient;
//import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
//import org.eclipse.paho.client.mqttv3.MqttException;
//import org.eclipse.paho.client.mqttv3.MqttMessage;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import br.com.ufu.iot_router_backend.model.MessageData;
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//
//@Service
//public class MqttClientService {
//
//    private IMqttClient client;
//
//    @Value("${mqtt.broker}")
//    private String broker;
//
//    @Value("${mqtt.clientId}")
//    private String clientId;
//
//    private Queue<MessageData> messageQueue = new ConcurrentLinkedQueue<>();
//    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
//
//    @PostConstruct
//    private void init() {
//        try {
//            this.client = new MqttClient(broker, clientId);
//            connectWithRetries();
//        } catch (MqttException e) {
//            System.err.println("Erro ao inicializar o cliente MQTT: " + e.getMessage());
//        }
//    }
//
//    private void connectWithRetries() {
//        scheduler.scheduleWithFixedDelay(() -> {
//            if (!client.isConnected()) {
//                try {
//                    connect();
//                    while (!messageQueue.isEmpty()) {
//                        MessageData message = messageQueue.poll();
//                        publish(message.getTopic(), message.getPayload());
//                    }
//                } catch (MqttException e) {
//                    System.err.println("Falha na conexão com o broker MQTT. Tentando novamente em 5 segundos.");
//                }
//            }
//        }, 0, 30, TimeUnit.SECONDS);
//    }
//
//    private void connect() throws MqttException {
//        MqttConnectOptions options = new MqttConnectOptions();
//        options.setAutomaticReconnect(true);
//        options.setCleanSession(true);
//        options.setMaxInflight(100);
//        options.setKeepAliveInterval(60);
//        client.connect(options);
//        System.out.println("Conectado ao broker MQTT com sucesso!");
//    }
//
//    public void publish(String topic, String payload) {
//        MqttMessage message = new MqttMessage(payload.getBytes());
//        message.setQos(0);
//        
//        if (messageQueue.size() > 100) {
//            System.out.println("Fila de mensagens muito grande. Reduzindo ou descartando mensagens.");
//        }
//
//        if (!client.isConnected()) {
//            System.out.println("Broker MQTT indisponível. Armazenando mensagem para envio posterior.");
//            messageQueue.add(new MessageData(topic, payload));
//            return;
//        }
//
//        try {
//            client.publish(topic, message);
//            System.out.println("Mensagem publicada no tópico: " + topic);
//        } catch (MqttException e) {
//            System.out.println("Erro ao publicar mensagem: " + e.getMessage());
//            messageQueue.add(new MessageData(topic, payload));
//        }
//    }
//
//    @PreDestroy
//    public void disconnect() {
//        scheduler.shutdown();
//        try {
//            if (client != null && client.isConnected()) {
//                client.disconnect();
//                System.out.println("Desconectado do broker MQTT");
//            }
//        } catch (MqttException e) {
//            System.err.println("Erro ao desconectar: " + e.getMessage());
//        }
//    }
//}
