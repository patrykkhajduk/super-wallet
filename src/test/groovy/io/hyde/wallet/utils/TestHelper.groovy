package io.hyde.wallet.utils

import io.hyde.wallet.BaseIntegrationTest
import io.hyde.wallet.domain.model.ExecutedCommand
import io.hyde.wallet.domain.model.Token
import io.hyde.wallet.domain.model.Wallet
import io.hyde.wallet.domain.model.command.WalletCommand
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.WalletCommandEvent
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.ErrorEvent
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.FundsAddedEvent
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.FundsBlockedEvent
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.FundsReleasedEvent
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.FundsWithdrawnEvent
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.WalletEvent
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.WalletSnapshot
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

@Component
class TestHelper {

    private final Clock clock
    private final TokenRepository tokenRepository
    private final WalletRepository walletRepository
    private final ExecutedCommandRepository executedCommandRepository
    private final ReactiveKafkaProducerTemplate<String, WalletCommandEvent> walletEventsProducer
    private final Flux<ReceiverRecord<String, WalletEvent>> walletEvents
    private final Flux<ReceiverRecord<String, WalletCommandEvent>> walletCommandsDltEvents

    TestHelper(Clock clock,
               KafkaProperties kafkaProperties,
               TokenRepository tokenRepository,
               WalletRepository walletRepository,
               ExecutedCommandRepository executedCommandRepository) {
        this.clock = clock
        this.tokenRepository = tokenRepository
        this.walletRepository = walletRepository
        this.executedCommandRepository = executedCommandRepository
        this.walletEventsProducer = new ReactiveKafkaProducerTemplate<>(
                SenderOptions.create(kafkaProperties.buildProducerProperties()))

        ReceiverOptions<String, WalletEvent> walletCommandsReceiverOptions =
                ReceiverOptions.<String, WalletEvent> create(kafkaProperties.buildConsumerProperties())
                        .subscription(Collections.singletonList(BaseIntegrationTest.WALLET_EVENTS_TOPIC));

        this.walletEvents = new ReactiveKafkaConsumerTemplate<>(walletCommandsReceiverOptions).receive()

        ReceiverOptions<String, WalletCommandEvent> walletCommandsDltReceiverOptions =
                ReceiverOptions.<String, WalletCommandEvent> create(kafkaProperties.buildConsumerProperties())
                        .subscription(Collections.singletonList(BaseIntegrationTest.WALLET_COMMANDS_DEAD_LETTER_TOPIC));

        this.walletCommandsDltEvents = new ReactiveKafkaConsumerTemplate<>(walletCommandsDltReceiverOptions).receive()
    }

    void clearData() {
        tokenRepository.deleteAll().block(Duration.ofSeconds(1))
        walletRepository.deleteAll().block(Duration.ofSeconds(1))
        executedCommandRepository.deleteAll().block(Duration.ofSeconds(1))
    }

    Token initToken(String token) {
        return tokenRepository.save(Token.ofName(token)).block(Duration.ofSeconds(1))
    }

    Wallet initWallet(String ownerId = UUID.randomUUID().toString()) {
        return walletRepository.save(Wallet.forOwner(ownerId)).block(Duration.ofSeconds(1))
    }

    Wallet executeAndStoreSendCommands(Wallet wallet, WalletCommand... commands) {
        commands.each {
            wallet.execute(it, clock)
            ExecutedCommand executedCommand = ExecutedCommand.fromLastExecutedCommand(wallet)
            executedCommand.markAsSend()
            wallet = executedCommandRepository.save(executedCommand)
                    .then(walletRepository.save(wallet))
                    .block(Duration.ofSeconds(1))
        }
        return wallet
    }

    Wallet executeAndStoreNotSendCommands(Wallet wallet, WalletCommand... commands) {
        commands.each {
            wallet.execute(it, clock)
            wallet = executedCommandRepository.save(ExecutedCommand.fromLastExecutedCommand(wallet))
                    .then(walletRepository.save(wallet))
                    .block(Duration.ofSeconds(1))
        }
        return wallet
    }

    Wallet executeWithoutStoringCommand(Wallet wallet, WalletCommand command) {
        wallet.execute(command, clock)
        return walletRepository.save(wallet).block(Duration.ofSeconds(1))
    }

    List<Token> getAllTokens() {
        return tokenRepository.findAll().collectList().block(Duration.ofSeconds(1))
    }

    long getAllTokensCount() {
        return tokenRepository.count().block(Duration.ofSeconds(1))
    }

    List<Wallet> getAllWallets() {
        return walletRepository.findAll().collectList().block(Duration.ofSeconds(1))
    }

    Wallet getWallet(String walletId) {
        return walletRepository.findById(walletId).block(Duration.ofSeconds(1))
    }

    long getAllWalletsCount() {
        return walletRepository.count().block(Duration.ofSeconds(1))
    }

    long getExecutedCommandsCount() {
        return executedCommandRepository.count().block(Duration.ofSeconds(1))
    }

    void sendWalletCommandEvents(WalletCommandEvent... events) {
        sendWalletCommandEventsFlux(Flux.fromArray(events))
    }

    void sendWalletCommandEvents(List<WalletCommandEvent> events) {
        sendWalletCommandEventsFlux(Flux.fromIterable(events))
    }

    private void sendWalletCommandEventsFlux(Flux<WalletCommandEvent> events) {
        events.flatMap(event -> walletEventsProducer.send(BaseIntegrationTest.WALLET_COMMANDS_TOPIC, event.walletId(), event))
                .blockLast()
    }

    void verifyFundsAddedEvent(String expectedId,
                               String expectedToken,
                               BigDecimal expectedAmount,
                               WalletSnapshot expectedWallet) {
        WalletEvent event = getNextWalletEvent()
        assert event instanceof FundsAddedEvent

        FundsAddedEvent fundsAddedEvent = event as FundsAddedEvent
        assert fundsAddedEvent.id() == expectedId
        assert fundsAddedEvent.token() == expectedToken
        assert fundsAddedEvent.amount() == expectedAmount
        assert fundsAddedEvent.wallet() == expectedWallet
        assert fundsAddedEvent.updatedAt() != null
    }

    String verifyFundsBlockedEvent(String expectedId,
                                   String expectedToken,
                                   BigDecimal expectedAmount,
                                   WalletSnapshot expectedWallet) {
        WalletEvent event = getNextWalletEvent()
        assert event instanceof FundsBlockedEvent

        FundsBlockedEvent fundsBlockedEvent = event as FundsBlockedEvent
        assert fundsBlockedEvent.id() == expectedId
        assert fundsBlockedEvent.token() == expectedToken
        assert fundsBlockedEvent.amount() == expectedAmount
        assert fundsBlockedEvent.blockedFundsLockId() != null
        assert fundsBlockedEvent.wallet() == expectedWallet
        assert fundsBlockedEvent.updatedAt() != null

        return fundsBlockedEvent.blockedFundsLockId()
    }

    void verifyFundsReleasedEvent(String expectedId,
                                  String expectedLockId,
                                  String expectedReleasedFundsToken,
                                  BigDecimal expectedReleasedFundsAmount,
                                  WalletSnapshot expectedWallet) {
        WalletEvent event = getNextWalletEvent()
        assert event instanceof FundsReleasedEvent

        FundsReleasedEvent fundsReleasedEvent = event as FundsReleasedEvent
        assert fundsReleasedEvent.id() == expectedId
        assert fundsReleasedEvent.lockId() == expectedLockId
        assert fundsReleasedEvent.releasedFundsToken() == expectedReleasedFundsToken
        assert fundsReleasedEvent.releasedFundsAmount() == expectedReleasedFundsAmount
        assert fundsReleasedEvent.wallet() == expectedWallet
        assert fundsReleasedEvent.updatedAt() != null
    }

    void verifyFundsWithdrawnEvent(String expectedId,
                                   String expectedLockId,
                                   String expectedWithdrawnFundsToken,
                                   BigDecimal expectedWithdrawnFundsAmount,
                                   WalletSnapshot expectedWallet) {
        WalletEvent event = getNextWalletEvent()
        assert event instanceof FundsWithdrawnEvent

        FundsWithdrawnEvent fundsWithdrawnEvent = event as FundsWithdrawnEvent
        assert fundsWithdrawnEvent.id() == expectedId
        assert fundsWithdrawnEvent.lockId() == expectedLockId
        assert fundsWithdrawnEvent.withdrawnFundsToken() == expectedWithdrawnFundsToken
        assert fundsWithdrawnEvent.withdrawnFundsAmount() == expectedWithdrawnFundsAmount
        assert fundsWithdrawnEvent.wallet() == expectedWallet
        assert fundsWithdrawnEvent.updatedAt() != null
    }

    void verifyErrorEvent(String expectedCommandId,
                          String expectedWalletId,
                          String expectedErrorMessage) {
        WalletEvent event = getNextWalletEvent()
        assert event instanceof ErrorEvent

        ErrorEvent errorEvent = event as ErrorEvent
        assert errorEvent.commandId() == expectedCommandId
        assert errorEvent.walletId() == expectedWalletId
        assert errorEvent.errorMessage() == expectedErrorMessage
    }

    private WalletEvent getNextWalletEvent() {
        return walletEvents.next()
                .flatMap(record -> record.receiverOffset().commit()
                        .then(Mono.just(record.value())))
                .block(Duration.ofSeconds(5))
    }

    void verifyNoEventSent() {
        assert walletEvents.next()
                .timeout(Duration.ofSeconds(3), Mono.empty())
                .block() == null
    }

    String getNextDltWalletCommandEvent() {
        return walletCommandsDltEvents.next()
                .flatMap(record -> record.receiverOffset().commit()
                        .then(Mono.just(record.value().id())))
                .block(Duration.ofSeconds(5))
    }

    void verifyExecutedCommandIsMarkedAsSend(String commandId, String walletId) {
        assert executedCommandRepository.findByWalletIdAndCommandId(walletId, commandId)
                .block(Duration.ofSeconds(1))
                .isSend()
    }

    void verifyExecutedCommandIsMarkedAsNotSend(String commandId, String walletId) {
        assert !executedCommandRepository.findByWalletIdAndCommandId(walletId, commandId)
                .block(Duration.ofSeconds(1))
                .isSend()
    }
}
