package io.hyde.wallet.domain.job

import io.hyde.wallet.application.ports.output.ExecutedCommandPort
import io.hyde.wallet.application.ports.output.WalletEventsPort
import io.hyde.wallet.domain.model.ExecutedCommand
import org.springframework.data.domain.PageRequest
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.lang.Subject

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class SendExecutedCommandsEventsJobTest extends Specification {

    private Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
    private ExecutedCommandPort executedCommandPort = Mock()
    private WalletEventsPort walletEventsPort = Mock()

    private int pageSize = 5
    private Duration jobTimeout = Duration.ofSeconds(1)
    private Duration eventsCreationDateDelay = Duration.ofSeconds(5)
    private LocalDateTime eventsDelayedCreationDate = LocalDateTime.now(clock).minus(eventsCreationDateDelay)

    @Subject
    private SendExecutedCommandsEventsJob job = new SendExecutedCommandsEventsJob(
            clock, pageSize, eventsCreationDateDelay, jobTimeout, executedCommandPort, walletEventsPort)

    def "should send executed commands events"() {
        given:
        ExecutedCommand command1 = build("command1", "wallet1")
        ExecutedCommand command2 = build("command2", "wallet2")
        ExecutedCommand command3 = build("command3", "wallet3")

        and:
        executedCommandPort.findAllBySendAndCreatedDateLessThanOrderByCreatedDateAsc(
                false, eventsDelayedCreationDate, PageRequest.of(0, pageSize)) >>
                Flux.fromIterable([command1, command2, command3])

        when:
        job.sendExecutedCommandsEvents()

        then:
        1 * walletEventsPort.sendEventFromExecutedCommand(command1) >> Mono.empty()
        1 * command1.markAsSend()
        1 * executedCommandPort.save(command1) >> Mono.just(command1)
        1 * walletEventsPort.sendEventFromExecutedCommand(command2) >> Mono.empty()
        1 * command2.markAsSend()
        1 * executedCommandPort.save(command2) >> Mono.just(command2)
        1 * walletEventsPort.sendEventFromExecutedCommand(command3) >> Mono.empty()
        1 * command3.markAsSend()
        1 * executedCommandPort.save(command3) >> Mono.just(command3)
    }

    def "should stop sending events if any command fails within wallet id group"() {
        given:
        ExecutedCommand command1 = build("command1", "wallet1")
        ExecutedCommand command2 = build("command2", "wallet1")
        ExecutedCommand command3 = build("command3", "wallet3")
        ExecutedCommand command4 = build("command4", "wallet4")

        and:
        executedCommandPort.findAllBySendAndCreatedDateLessThanOrderByCreatedDateAsc(
                false, eventsDelayedCreationDate, PageRequest.of(0, pageSize)) >>
                Flux.fromIterable([command1, command2, command3, command4])

        when:
        job.sendExecutedCommandsEvents()

        then:
        1 * walletEventsPort.sendEventFromExecutedCommand(command1) >> Mono.empty()
        1 * command1.markAsSend()
        1 * executedCommandPort.save(command1) >> Mono.error(new RuntimeException("Exception for tests"))
        0 * walletEventsPort.sendEventFromExecutedCommand(command2) >> Mono.empty()
        0 * command2.markAsSend()
        0 * executedCommandPort.save(command2) >> Mono.just(command2)
        1 * walletEventsPort.sendEventFromExecutedCommand(command3) >> Mono.empty()
        1 * command3.markAsSend()
        1 * executedCommandPort.save(command3) >> Mono.just(command3)
        1 * walletEventsPort.sendEventFromExecutedCommand(command4) >> Mono.empty()
        1 * command4.markAsSend()
        1 * executedCommandPort.save(command4) >> Mono.just(command4)
    }

    def "should stop sending events after timeout"() {
        given:
        ExecutedCommand command1 = build("command1", "wallet1")
        ExecutedCommand command2 = build("command2", "wallet1")

        and:
        executedCommandPort.findAllBySendAndCreatedDateLessThanOrderByCreatedDateAsc(
                false, eventsDelayedCreationDate, PageRequest.of(0, pageSize)) >>
                Flux.fromIterable([command1, command2])

        when:
        job.sendExecutedCommandsEvents()

        then:
        1 * walletEventsPort.sendEventFromExecutedCommand(command1) >> Mono.delay(jobTimeout.plusSeconds(1)).then(Mono.just(command2))
        0 * walletEventsPort.sendEventFromExecutedCommand(command2) >> Mono.empty()
    }

    private ExecutedCommand build(String commandId, String walletId) {
        ExecutedCommand command = Mock()
        command.getCommandId() >> commandId
        command.getWalletId() >> walletId
        return command
    }
}
