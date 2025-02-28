package io.hyde.wallet.infrastructure.adapters.output.messaging.producer;

import io.hyde.wallet.application.ports.output.WalletEventsPort;
import io.hyde.wallet.domain.model.ExecutedCommand;
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.WalletEvent;
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.mapper.WalletEventMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderOptions;

@Slf4j
@Component
class KafkaWalletEventProducer implements WalletEventsPort {

    private final ReactiveKafkaProducerTemplate<String, WalletEvent> kafkaProducer;
    private final String walletEventsTopic;

    public KafkaWalletEventProducer(KafkaProperties properties,
                                    @Value("${spring.kafka.producer.topics.wallet-events.name}") String walletEventsTopic) {
        this.kafkaProducer = new ReactiveKafkaProducerTemplate<>(SenderOptions.create(properties.buildProducerProperties()));
        this.walletEventsTopic = walletEventsTopic;
    }

    @Override
    public Mono<Void> sendEventFromExecutedCommand(ExecutedCommand executedCommand) {
        log.info("Sending wallet update event for wallet: {} and last executed command: {}",
                executedCommand.getWalletId(), executedCommand.getCommandResult());
        return sendEvent(WalletEventMapper.map(executedCommand));
    }

    @Override
    public Mono<Void> sendEvent(WalletEvent event) {
        log.info("Sending {} for wallet: {}", event.getClass().getSimpleName(), event.walletId());
        return kafkaProducer.send(walletEventsTopic, event.walletId(), event)
                .then();
    }
}
