package io.hyde.wallet.infrastructure.adapters.input.messaging.config;

import io.hyde.wallet.application.ports.input.ProcessWalletCommandUseCase;
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.WalletCommandEvent;
import io.hyde.wallet.infrastructure.adapters.input.messaging.listener.KafkaWalletCommandEventsReactiveListener;
import io.hyde.wallet.utils.validation.AnnotationProcessingValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Flux;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.SenderOptions;

import java.time.Duration;
import java.util.Collections;

@Configuration
class KafkaReactiveListenerConfiguration {

    @Bean
    KafkaWalletCommandEventsReactiveListener kafkaWalletCommandEventsReactiveListener(
            KafkaProperties kafkaProperties,
            @Value("${spring.kafka.consumer.topics.wallet-commands.name}") String walletCommandsTopic,
            @Value("${spring.kafka.consumer.topics.wallet-commands.processing-timeout}") Duration processingTimeout,
            @Value("${spring.kafka.consumer.topics.wallet-commands.retry-count}") int retryCount,
            @Value("${spring.kafka.consumer.topics.wallet-commands.retry-delay}") Duration retryDelay,
            AnnotationProcessingValidator annotationProcessingValidator,
            ProcessWalletCommandUseCase processWalletCommandUseCase) {
        ReceiverOptions<String, WalletCommandEvent> receiverOptions =
                ReceiverOptions.<String, WalletCommandEvent>create(kafkaProperties.buildConsumerProperties())
                        .subscription(Collections.singletonList(walletCommandsTopic));
        Flux<ReceiverRecord<String, WalletCommandEvent>> records =
                new ReactiveKafkaConsumerTemplate<>(receiverOptions).receive();

        ReactiveKafkaProducerTemplate<String, WalletCommandEvent> dltProducer =
                new ReactiveKafkaProducerTemplate<>(SenderOptions.create(kafkaProperties.buildProducerProperties()));

        return new KafkaWalletCommandEventsReactiveListener(
                processingTimeout,
                retryCount,
                retryDelay,
                records,
                annotationProcessingValidator,
                processWalletCommandUseCase,
                walletCommandsTopic + "-dlt",
                dltProducer);
    }
}
