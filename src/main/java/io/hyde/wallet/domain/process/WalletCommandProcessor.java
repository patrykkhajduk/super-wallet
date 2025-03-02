package io.hyde.wallet.domain.process;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import io.hyde.wallet.application.ports.input.ExecuteProcessStepsUseCase;
import io.hyde.wallet.application.ports.input.FindWalletsUseCase;
import io.hyde.wallet.application.ports.input.ProcessWalletCommandUseCase;
import io.hyde.wallet.application.ports.input.UpdateWalletUseCase;
import io.hyde.wallet.application.ports.output.WalletEventsPort;
import io.hyde.wallet.application.ports.output.WalletProcessPort;
import io.hyde.wallet.domain.exception.ApplicationException;
import io.hyde.wallet.domain.exception.WalletCommandProcessingException;
import io.hyde.wallet.domain.model.Wallet;
import io.hyde.wallet.domain.model.command.WalletCommand;
import io.hyde.wallet.domain.model.process.WalletProcess;
import io.hyde.wallet.domain.model.process.WalletProcess.WalletProcessStep;
import io.hyde.wallet.domain.process.step.WalletStepProcessor;
import io.hyde.wallet.domain.validator.WalletCommandValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
class WalletCommandProcessor implements ProcessWalletCommandUseCase, ExecuteProcessStepsUseCase {

    private final Clock clock;
    private final WalletCommandValidator walletCommandValidator;
    private final Map<WalletProcessStep, WalletStepProcessor> stepProcessors;
    private final FindWalletsUseCase findWalletsUseCase;
    private final UpdateWalletUseCase updateWalletUseCase;
    private final WalletProcessPort walletProcessPort;
    private final WalletEventsPort walletEventsPort;

    public WalletCommandProcessor(Clock clock,
                                  WalletCommandValidator walletCommandValidator,
                                  Collection<WalletStepProcessor> stepProcessors,
                                  FindWalletsUseCase findWalletsUseCase,
                                  UpdateWalletUseCase updateWalletUseCase,
                                  WalletProcessPort walletProcessPort,
                                  WalletEventsPort walletEventsPort) {
        this.clock = clock;
        this.walletCommandValidator = walletCommandValidator;
        this.findWalletsUseCase = findWalletsUseCase;
        this.updateWalletUseCase = updateWalletUseCase;
        this.walletProcessPort = walletProcessPort;

        this.stepProcessors = stepProcessors
                .stream()
                .collect(Collectors.toMap(WalletStepProcessor::getStep, Function.identity()));
        this.walletEventsPort = walletEventsPort;
        Set<WalletProcessStep> missingSteps = Sets.difference(
                Sets.newHashSet(WalletProcessStep.values()), this.stepProcessors.keySet());
        Preconditions.checkState(missingSteps.isEmpty(), "Missing processors for steps: " + missingSteps);
    }

    @Override
    public Mono<Void> process(WalletCommand command) {
        return getWallet(command.getWalletId())
                .flatMap(wallet -> walletProcessPort.existsByCommandIdAndWalletIdAndCompleted(command.getId(), wallet.getId(), true)
                        .flatMap(completedProcessExists -> {
                            if (completedProcessExists) {
                                log.info("Command {} already executed for wallet {}", command.getId(), wallet.getId());
                                return Mono.empty();
                            } else {
                                return processNotExecutedCommand(wallet, command);
                            }
                        }))
                .onErrorResume(t -> processError(command.getId(), command.getWalletId(), t));
    }

    @Override
    public Mono<Void> processSteps(WalletProcess process) {
        return getWallet(process.getWalletId())
                .flatMap(wallet -> processSteps(new WalletProcessData(wallet, process)));
    }

    private Mono<Wallet> getWallet(String walletId) {
        return findWalletsUseCase.findWallet(walletId)
                .switchIfEmpty(Mono.error(new WalletCommandProcessingException("Wallet not found: " + walletId)));
    }

    private Mono<Void> processNotExecutedCommand(Wallet wallet, WalletCommand command) {
        return walletProcessPort.existsByWalletIdAndCompleted(wallet.getId(), false)
                .flatMap(notCompletedProcessExists -> {
                    Mono<WalletProcess> walletProcess = resolveWalletProcess(wallet, command);
                    if (notCompletedProcessExists) {
                        log.info("Unfinished process for command {} and wallet {} exists, not progressing process",
                                command.getId(), wallet.getId());
                        return walletProcess.then();
                    } else {
                        return walletProcess.flatMap(savedProcess ->
                                processSteps(new WalletProcessData(wallet, savedProcess)));
                    }
                });
    }

    private Mono<WalletProcess> resolveWalletProcess(Wallet wallet, WalletCommand command) {
        return walletCommandValidator.validate(command)
                .then(walletProcessPort.findByCommandIdAndWalletId(command.getId(), wallet.getId()))
                .switchIfEmpty(walletProcessPort.save(WalletProcess.fromWalletCommand(command, wallet)));
    }

    private Mono<Void> processSteps(WalletProcessData data) {
        return data.process()
                .getNextStep()
                .map(step -> processStep(data, step))
                .orElseGet(Mono::empty)
                .doOnSuccess(v -> log.info("Process {} for wallet {} completed", data.process().getId(), data.wallet().getId()));
    }

    private Mono<Void> processStep(WalletProcessData data, WalletProcessStep step) {
        log.info("Processing step {} of process {} for wallet {} command {}",
                step, data.process().getId(), data.wallet().getId(), data.process().getWalletCommand().getId());
        return stepProcessors.get(step)
                .executeStep(data)
                .flatMap(d -> {
                    d.process().markStepAsCompleted(step, clock);
                    return storeData(d);
                })
                .flatMap(this::processSteps)
                .onErrorResume(t -> processError(data.process().getWalletCommand().getId(), data.wallet().getId(), t));
    }

    private Mono<WalletProcessData> storeData(WalletProcessData data) {
        return updateWalletUseCase.updateWallet(data.wallet())
                .flatMap(w -> walletProcessPort.save(data.process())
                        .map(p -> new WalletProcessData(w, p)))
                .doOnSuccess(d ->
                        log.info("Data stored for process {} and wallet {}",
                                d.process().getId(), d.wallet().getId()));
    }

    private Mono<Void> processError(String commandId, String walletId, Throwable throwable) {
        return switch (throwable) {
            case ApplicationException applicationException -> {
                log.warn("Application exception occurred while processing command: {}, error: {}",
                        commandId, applicationException.getMessage());
                yield walletEventsPort.sendErrorEvent(commandId, walletId, applicationException.getMessage());
            }
            default -> {
                log.error("Error occurred while processing command: {} for wallet: {}",
                        commandId, walletId, throwable);
                yield Mono.error(throwable);
            }
        };
    }
}
