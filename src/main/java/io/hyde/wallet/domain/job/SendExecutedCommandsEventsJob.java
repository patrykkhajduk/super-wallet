package io.hyde.wallet.domain.job;

import io.hyde.wallet.application.ports.output.ExecutedCommandPort;
import io.hyde.wallet.application.ports.output.WalletEventsPort;
import io.hyde.wallet.domain.model.ExecutedCommand;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class SendExecutedCommandsEventsJob {

    private final Clock clock;
    private final Integer pageSize;
    private final Duration eventsCreationDateDelay;
    private final Duration jobTimeout;
    private final ExecutedCommandPort executedCommandPort;
    private final WalletEventsPort walletEventsPort;

    public SendExecutedCommandsEventsJob(Clock clock,
                                         @Value("${jobs.send-executed-commands.page-size}") Integer pageSize,
                                         @Value("${jobs.send-executed-commands.events-creation-date-delay}") Duration eventsCreationDateDelay,
                                         @Value("${jobs.send-executed-commands.timeout}") Duration jobTimeout,
                                         ExecutedCommandPort executedCommandPort,
                                         WalletEventsPort walletEventsPort) {
        this.clock = clock;
        this.pageSize = pageSize;
        this.eventsCreationDateDelay = eventsCreationDateDelay;
        this.jobTimeout = jobTimeout;
        this.executedCommandPort = executedCommandPort;
        this.walletEventsPort = walletEventsPort;
    }

    @Scheduled(cron = "${jobs.send-executed-commands.cron}")
    @SchedulerLock(name = "sendExecutedCommandsEvents", lockAtMostFor = "${jobs.send-executed-commands.timeout}")
    public void sendExecutedCommandsEvents() {
        executedCommandPort.findAllBySendAndCreatedDateLessThanOrderByCreatedDateAsc(
                        false, LocalDateTime.now(clock).minus(eventsCreationDateDelay), PageRequest.of(0, pageSize))
                .groupBy(ExecutedCommand::getWalletId)
                .flatMap(Flux::collectList)
                .flatMap(this::sendCommands)
                .take(jobTimeout)
                .blockLast(jobTimeout);
    }

    private Flux<ExecutedCommand> sendCommands(List<ExecutedCommand> commands) {
        return Flux.fromIterable(commands)
                .flatMap(this::sendCommand)
                .onErrorComplete();
    }

    private Mono<ExecutedCommand> sendCommand(ExecutedCommand command) {
        log.info("Sending executed command: {}", command.getCommandId());
        return walletEventsPort.sendEventFromExecutedCommand(command)
                .then(markExecutedCommandAsSend(command))
                .doOnSuccess(c -> log.info("Executed command: {} sent", c.getCommandId()))
                .doOnError(t -> log.error("Error sending executed command: {}", command.getCommandId(), t));
    }

    private Mono<ExecutedCommand> markExecutedCommandAsSend(ExecutedCommand executedCommand) {
        log.info("Marking executed command as send: {}", executedCommand.getId());
        executedCommand.markAsSend();
        return executedCommandPort.save(executedCommand);
    }
}
