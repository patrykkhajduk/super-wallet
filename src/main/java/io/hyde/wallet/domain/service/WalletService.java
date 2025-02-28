package io.hyde.wallet.domain.service;

import io.hyde.wallet.application.dto.CreateWalletRequestDto;
import io.hyde.wallet.application.ports.input.CreateWalletUseCase;
import io.hyde.wallet.application.ports.input.FindWalletsUseCase;
import io.hyde.wallet.application.ports.input.ProcessWalletCommandUseCase;
import io.hyde.wallet.application.ports.output.TokenPort;
import io.hyde.wallet.application.ports.output.WalletEventsPort;
import io.hyde.wallet.application.ports.output.WalletPort;
import io.hyde.wallet.domain.exception.ApplicationException;
import io.hyde.wallet.domain.exception.ValidationException;
import io.hyde.wallet.domain.exception.WalletCommandProcessingException;
import io.hyde.wallet.domain.model.Wallet;
import io.hyde.wallet.domain.model.command.WalletCommand;
import io.hyde.wallet.domain.model.command.WalletTokenRelatedCommand;
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.ErrorEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Clock;

@Slf4j
@Service
class WalletService implements FindWalletsUseCase, CreateWalletUseCase, ProcessWalletCommandUseCase {

    private final int walletsLimit;
    private final Clock clock;
    private final TokenPort tokenPort;
    private final WalletPort walletPort;
    private final WalletEventsPort walletEventsPort;
    private final ExecutedCommandService executedCommandService;

    public WalletService(@Value("${wallets.limit-per-owner}") Integer walletsLimit,
                         Clock clock,
                         TokenPort tokenPort,
                         WalletPort walletPort,
                         WalletEventsPort walletEventsPort,
                         ExecutedCommandService executedCommandService) {
        this.walletsLimit = walletsLimit;
        this.clock = clock;
        this.tokenPort = tokenPort;
        this.walletPort = walletPort;
        this.walletEventsPort = walletEventsPort;
        this.executedCommandService = executedCommandService;
    }

    @Override
    public Mono<Page<Wallet>> findAllWallets(Pageable pageable) {
        return walletPort.findAllBy(pageable)
                .collectList()
                .zipWith(walletPort.count())
                .map(pair -> new PageImpl<>(pair.getT1(), pageable, pair.getT2()));
    }

    @Override
    public Mono<Wallet> findWallet(String id) {
        return walletPort.findById(id);
    }

    @Override
    public Mono<Wallet> createWallet(CreateWalletRequestDto request) {
        return walletPort.countByOwnerId(request.ownerId())
                .filter(count -> count < walletsLimit)
                .switchIfEmpty(Mono.error(new ValidationException("Wallets limit reached for owner: " + request.ownerId())))
                .then(walletPort.save(request.toWallet()))
                .doOnSuccess(wallet -> log.info("Wallet created: {}", wallet.getId()));
    }

    @Override
    public Mono<Void> process(WalletCommand command) {
        return executedCommandService.isCommandAlreadyProcessedForWallet(command.id(), command.walletId())
                .flatMap(executedCommandExists -> {
                    if (executedCommandExists) {
                        log.info("Command already executed: {}", command.id());
                        return Mono.empty();
                    } else {
                        return processNotExecutedCommand(command);
                    }
                });
    }

    private Mono<Void> processNotExecutedCommand(WalletCommand command) {
        return applyNotExecutedCommand(command)
                .doOnSuccess(v -> log.info("Command executed: {}", command.id()))
                .onErrorResume(throwable ->
                        switch (throwable) {
                            case ApplicationException applicationException -> {
                                log.warn("Application exception occurred while processing command: {}, error: {}",
                                        command.id(), applicationException.getMessage());
                                yield walletEventsPort.sendEvent(ErrorEvent.from(command, applicationException));
                            }
                            default -> {
                                log.error("Error occurred while processing command: {}", command.id(), throwable);
                                yield Mono.error(throwable);
                            }
                        });
    }

    private Mono<Void> applyNotExecutedCommand(WalletCommand command) {
        log.info("Processing {} {}", command.getClass().getSimpleName(), command.id());
        return switch (command) {
            case WalletTokenRelatedCommand walletTokenRelatedCommand ->
                    validateToken(walletTokenRelatedCommand.token()).then(executeCommand(command));
            default -> executeCommand(command);
        };
    }

    private Mono<Void> validateToken(String token) {
        return tokenPort.existsByName(token)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new WalletCommandProcessingException("Token not found: " + token)))
                .then();
    }

    private Mono<Void> executeCommand(WalletCommand command) {
        return getWallet(command)
                .flatMap(executedCommandService::sendLastExecutedCommandIfMissing)
                .filter(wallet -> isCommandNotSameAsLastExecuted(wallet, command))
                .flatMap(wallet -> executeCommandAndStoreWallet(wallet, command))
                .flatMap(executedCommandService::storeAndSendExecutedCommand);
    }

    private boolean isCommandNotSameAsLastExecuted(Wallet wallet, WalletCommand command) {
        boolean areCommandsIdsEqual = wallet.getLastExecutedCommandId()
                .map(command.id()::equals)
                .orElse(false);
        if (areCommandsIdsEqual) {
            log.info("Command {} is same as last executed in wallet: {}, ignoring", command.id(), wallet.getId());
            return false;
        } else {
            return true;
        }
    }

    private Mono<Wallet> getWallet(WalletCommand command) {
        return findWallet(command.walletId())
                .switchIfEmpty(Mono.error(new WalletCommandProcessingException("Wallet not found: " + command.walletId())));
    }

    private Mono<Wallet> executeCommandAndStoreWallet(Wallet wallet, WalletCommand command) {
        wallet.execute(command, clock);
        return walletPort.save(wallet);
    }
}
