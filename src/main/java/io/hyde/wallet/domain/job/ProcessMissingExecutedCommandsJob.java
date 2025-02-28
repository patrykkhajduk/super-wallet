package io.hyde.wallet.domain.job;

import io.hyde.wallet.application.ports.output.WalletPort;
import io.hyde.wallet.domain.service.ExecutedCommandService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
public class ProcessMissingExecutedCommandsJob {

    private final Clock clock;
    private final Duration walletsLastModifiedDateDelay;
    private final Duration jobTimeout;
    private final WalletPort walletPort;
    private final ExecutedCommandService executedCommandService;

    public ProcessMissingExecutedCommandsJob(
            Clock clock,
            @Value("${jobs.process-missing-executed-commands.wallets-last-modified-date-delay}") Duration walletsLastModifiedDateDelay,
            @Value("${jobs.process-missing-executed-commands.timeout}") Duration jobTimeout,
            WalletPort walletPort,
            ExecutedCommandService executedCommandService) {
        this.clock = clock;
        this.walletsLastModifiedDateDelay = walletsLastModifiedDateDelay;
        this.jobTimeout = jobTimeout;
        this.walletPort = walletPort;
        this.executedCommandService = executedCommandService;
    }

    @Scheduled(cron = "${jobs.process-missing-executed-commands.cron}")
    @SchedulerLock(name = "processMissingExecutedCommands", lockAtMostFor = "${jobs.process-missing-executed-commands.timeout}")
    public void processMissingExecutedCommands() {
        walletPort.findAllWithoutStoredLastExecutedCommand(LocalDateTime.now(clock).minus(walletsLastModifiedDateDelay))
                .flatMap(wallet -> executedCommandService.sendLastExecutedCommandIfMissing(wallet)
                        .doOnSuccess(w ->
                                log.info("Processed missing executed command: {} for wallet: {}",
                                        wallet.getLastExecutedCommandId().orElse(null), wallet.getId()))
                        .doOnError(t ->
                                log.error("Error while processing missing executed command: {} for wallet: {}",
                                        wallet.getLastExecutedCommandId().orElse(null), wallet.getId(), t)))
                .onErrorContinue((t, o) -> log.error("Error while processing missing executed commands", t))
                .take(jobTimeout)
                .blockLast(jobTimeout);
    }
}
