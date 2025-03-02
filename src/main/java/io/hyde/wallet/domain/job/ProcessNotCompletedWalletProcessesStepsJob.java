package io.hyde.wallet.domain.job;

import io.hyde.wallet.application.ports.input.ExecuteProcessStepsUseCase;
import io.hyde.wallet.application.ports.output.WalletProcessPort;
import io.hyde.wallet.domain.model.process.WalletProcess;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class ProcessNotCompletedWalletProcessesStepsJob {

    private final Clock clock;
    private final Duration eventsCreationDateDelay;
    private final Duration jobTimeout;
    private final WalletProcessPort walletProcessPort;
    private final ExecuteProcessStepsUseCase executeProcessStepsUseCase;

    public ProcessNotCompletedWalletProcessesStepsJob(Clock clock,
                                                      @Value("${jobs.process-not-completed-wallet-processes-steps.processes-creation-date-delay}") Duration processesCreationDateDelay,
                                                      @Value("${jobs.process-not-completed-wallet-processes-steps.timeout}") Duration jobTimeout,
                                                      WalletProcessPort walletProcessPort,
                                                      ExecuteProcessStepsUseCase executeProcessStepsUseCase) {
        this.clock = clock;
        this.eventsCreationDateDelay = processesCreationDateDelay;
        this.jobTimeout = jobTimeout;
        this.walletProcessPort = walletProcessPort;
        this.executeProcessStepsUseCase = executeProcessStepsUseCase;
    }

    @Scheduled(cron = "${jobs.process-not-completed-wallet-processes-steps.cron}")
    @SchedulerLock(name = "processNotCompletedWalletProcessesEvents", lockAtMostFor = "${jobs.process-not-completed-wallet-processes-steps.timeout}")
    public void processNotCompletedWalletProcessesEvents() {
        walletProcessPort.findAllByCompletedAndCreatedDateLessThanOrderByCreatedDateAsc(
                        false, LocalDateTime.now(clock).minus(eventsCreationDateDelay))
                .groupBy(WalletProcess::getWalletId)
                .flatMap(Flux::collectList)
                .flatMap(this::processSteps)
                .take(jobTimeout)
                .blockLast(jobTimeout);
    }

    private Flux<Void> processSteps(List<WalletProcess> processes) {
        return Flux.fromIterable(processes)
                .flatMapSequential(executeProcessStepsUseCase::processSteps, 1)
                .onErrorComplete();
    }
}
