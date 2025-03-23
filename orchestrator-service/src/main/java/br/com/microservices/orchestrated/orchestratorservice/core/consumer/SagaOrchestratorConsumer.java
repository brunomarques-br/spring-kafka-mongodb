package br.com.microservices.orchestrated.orchestratorservice.core.consumer;


import br.com.microservices.orchestrated.orchestratorservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class SagaOrchestratorConsumer {

    private final JsonUtil jsonUtil;

    // start-saga
    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.start-saga}"
    )
    public void consumeStartSagaEvent(String payload){
        log.info("SagaOrchestratorConsumer: Received event from start-saga topic with payload: {}", payload);
        var event = jsonUtil.toEvent(payload);
        log.info(event.toString());
    }

    // orchestrator
    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.orchestrator}"
    )
    public void consumeOrchestratorEvent(String payload){
        log.info("SagaOrchestratorConsumer: Received event from orchestrator topic with payload: {}", payload);
        var event = jsonUtil.toEvent(payload);
        log.info(event.toString());
    }

    // finish-success
    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.finish-success}"
    )
    public void consumeFinishSuccessEvent(String payload){
        log.info("SagaOrchestratorConsumer: Received event from finish-success topic with payload: {}", payload);
        var event = jsonUtil.toEvent(payload);
        log.info(event.toString());
    }

    // finish-fail
    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${spring.kafka.topic.finish-fail}"
    )
    public void consumeFinishFailEvent(String payload){
        log.info("SagaOrchestratorConsumer: Received event from finish-fail topic with payload: {}", payload);
        var event = jsonUtil.toEvent(payload);
        log.info(event.toString());
    }

}
