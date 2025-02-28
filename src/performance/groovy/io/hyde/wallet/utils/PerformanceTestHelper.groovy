package io.hyde.wallet.utils

import groovy.util.logging.Slf4j
import io.hyde.wallet.BasePerformanceTest
import io.hyde.wallet.domain.model.ExecutedCommand
import io.hyde.wallet.domain.model.Token
import io.hyde.wallet.domain.model.Wallet
import io.hyde.wallet.domain.model.command.WalletCommand
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.WalletCommandEvent
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.WalletEvent
import io.hyde.wallet.infrastructure.adapters.output.persistence.repository.ExecutedCommandRepository
import io.hyde.wallet.infrastructure.adapters.output.persistence.repository.TokenRepository
import io.hyde.wallet.infrastructure.adapters.output.persistence.repository.WalletRepository
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import reactor.kafka.sender.SenderOptions

import java.time.Clock
import java.time.Duration
import java.util.stream.IntStream

@Slf4j
@Component
class PerformanceTestHelper {

    private final Clock clock
    private final TokenRepository tokenRepository
    private final WalletRepository walletRepository
    private final ExecutedCommandRepository executedCommandRepository
    private final ReactiveKafkaProducerTemplate<String, WalletCommandEvent> walletEventsProducer
    private final Flux<ReceiverRecord<String, WalletEvent>> walletEvents

    PerformanceTestHelper(Clock clock,
                          KafkaProperties kafkaProperties,
                          TokenRepository tokenRepository,
                          ExecutedCommandRepository executedCommandRepository,
                          WalletRepository walletRepository) {
        this.clock = clock
        this.tokenRepository = tokenRepository
        this.walletRepository = walletRepository
        this.executedCommandRepository = executedCommandRepository
        this.walletEventsProducer = new ReactiveKafkaProducerTemplate<>(
                SenderOptions.create(kafkaProperties.buildProducerProperties()))

        ReceiverOptions<String, WalletEvent> walletCommandsReceiverOptions =
                ReceiverOptions.<String, WalletEvent> create(kafkaProperties.buildConsumerProperties())
                        .subscription(Collections.singletonList(BasePerformanceTest.WALLET_EVENTS_TOPIC));
        this.walletEvents = new ReactiveKafkaConsumerTemplate<>(walletCommandsReceiverOptions).receive()
    }

    void clearData() {
        tokenRepository.deleteAll().block(Duration.ofSeconds(1))
        walletRepository.deleteAll().block(Duration.ofSeconds(1))
        executedCommandRepository.deleteAll().block(Duration.ofSeconds(1))
    }

    Token initToken(String token) {
        return tokenRepository.save(Token.ofName(token)).block(Duration.ofSeconds(1))
    }

    Wallet initWallet() {
        return walletRepository.save(Wallet.forOwner(UUID.randomUUID().toString())).block(Duration.ofSeconds(1))
    }

    Mono<Void> initWallets(int count) {
        return walletRepository.saveAll(Flux.fromStream(IntStream.rangeClosed(1, count).boxed())
                .map(i -> Wallet.forOwner(UUID.randomUUID().toString())))
                .then()
    }

    Mono<Wallet> executeAndStoreSendCommands(Wallet wallet, Flux<WalletCommand> commandsFlux) {
        executedCommandRepository.saveAll(commandsFlux.map({ WalletCommand command ->
            wallet.execute(command, clock)
            return ExecutedCommand.fromLastExecutedCommand(wallet)
        })).then(walletRepository.save(wallet))
    }

    Wallet getWallet(String walletId) {
        return walletRepository.findById(walletId).block(Duration.ofSeconds(1))
    }

    long getWalletsCount() {
        return walletRepository.count().block(Duration.ofSeconds(1))
    }

    long getExecutedCommandsCount() {
        return executedCommandRepository.count().block(Duration.ofSeconds(1))
    }

    void sendWalletCommandEvent(WalletCommandEvent event) {
        walletEventsProducer.send(BasePerformanceTest.WALLET_COMMANDS_TOPIC, event.walletId(), event)
                .block(Duration.ofSeconds(1))
    }

    Mono<Void> sendWalletCommandEvents(Flux<WalletCommandEvent> events) {
        return events.flatMap(event ->
                walletEventsProducer.send(BasePerformanceTest.WALLET_COMMANDS_TOPIC, event.walletId(), event))
                .then()
    }
}
