package io.hyde.wallet.infrastructure.adapters.input.messaging.listener;

import io.hyde.wallet.application.ports.input.ProcessWalletCommandUseCase;
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.WalletCommandEvent;
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.mapper.WalletCommandEventMapper;
import io.hyde.wallet.utils.validation.AnnotationProcessingValidator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.validation.BindingResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public class KafkaWalletCommandEventsReactiveListener {

    private final Duration processingTimeout;
    private final int retryCount;
    private final Duration retryDelay;
    private final Flux<ReceiverRecord<String, WalletCommandEvent>> recordsProducer;
    private final AnnotationProcessingValidator annotationProcessingValidator;
    private final ProcessWalletCommandUseCase processWalletCommandUseCase;
    private final String walletCommandsDeadLetterTopic;
    private final ReactiveKafkaProducerTemplate<String, WalletCommandEvent> dltProducer;

    @PostConstruct
    void receive() {
        recordsProducer.retryWhen(Retry.indefinitely())
                .doOnNext(record -> {
                    //since we're consuming flux of events each event has to be timeout for processing before next event is consumed
                    //this is similar to visibility timeout in AWS SQS
                    processRecord(record).block(processingTimeout);
                }).repeat()
                .subscribe();
    }

    private Mono<Void> processRecord(ReceiverRecord<String, WalletCommandEvent> record) {
        BindingResult validationResult = annotationProcessingValidator.validate(record.value());
        if (validationResult.hasErrors()) {
            log.error("Validation error: {} {} with offset: {}, sending to DLT, errors: {}",
                    record.value().getClass().getSimpleName(),
                    record.value().id(),
                    record.receiverOffset().offset(),
                    validationResult.getAllErrors());
            return sendToDlt(record);
        } else {
            return processWalletCommandUseCase.process(WalletCommandEventMapper.map(record.value()))
                    .retryWhen(Retry.backoff(retryCount, retryDelay))
                    .doOnSuccess(v -> {
                        log.info("Processed event: {} {} with offset: {}, acknowledging",
                                record.value().getClass().getSimpleName(),
                                record.value().id(),
                                record.receiverOffset().offset());
                        record.receiverOffset().acknowledge();
                    }).doOnError(throwable -> sendToDlt(record));
        }
    }

    private Mono<Void> sendToDlt(ReceiverRecord<String, WalletCommandEvent> record) {
        return dltProducer.send(walletCommandsDeadLetterTopic, record.key(), record.value())
                .doOnSuccess(s -> {
                    log.info("{} {} (offset: {}) sent to DLT: {}",
                            record.value().getClass().getSimpleName(),
                            record.value().id(),
                            record.receiverOffset().offset(),
                            walletCommandsDeadLetterTopic);
                    record.receiverOffset().acknowledge();
                }).then();
    }
}
