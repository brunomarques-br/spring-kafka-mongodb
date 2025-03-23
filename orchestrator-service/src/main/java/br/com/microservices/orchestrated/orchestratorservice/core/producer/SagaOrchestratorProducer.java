package br.com.microservices.orchestrated.orchestratorservice.core.producer;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j // Lombok annotation to create a logger field
@Component // Spring annotation to indicate that this class is a Spring component
@AllArgsConstructor // Lombok annotation to create a constructor with all required fields
public class SagaOrchestratorProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendEvent(String payload, String topic) {
        try {
            log.info("SagaOrchestratorProducer: Sending message to topic: {} with data {}", topic, payload);
            kafkaTemplate.send(topic, payload);
        } catch (Exception e) {
            log.error("Error sending payload to topic: {} with data {}", topic, payload, e);
        }
    }

}
