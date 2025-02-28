package io.hyde.wallet.domain.service;

import io.hyde.wallet.application.ports.output.ExecutedCommandPort;
import io.hyde.wallet.application.ports.output.WalletEventsPort;
import io.hyde.wallet.domain.model.ExecutedCommand;
import io.hyde.wallet.domain.model.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutedCommandService {

    private final ExecutedCommandPort executedCommandPort;
    private final WalletEventsPort walletEventsPort;

    public Mono<Wallet> sendLastExecutedCommandIfMissing(Wallet wallet) {
        //Checking if last executed command was stored in case when previous command processing failed, and it was sent to DLT
        return wallet.getLastExecutedCommandId()
                .map(lastExecutedCommandId ->
                        isCommandAlreadyProcessedForWallet(lastExecutedCommandId, wallet.getId())
                                .flatMap(executedCommandExists -> {
                                    if (executedCommandExists) {
                                        return Mono.just(wallet);
                                    } else {
                                        return storeNotSendExecutedCommand(wallet)
                                                .flatMap(this::sendWalletUpdateEvent)
                                                .then(Mono.just(wallet));
                                    }
                                }))
                .orElseGet(() -> Mono.just(wallet));
    }

    public Mono<Boolean> isCommandAlreadyProcessedForWallet(String commandId, String walletId){
        return executedCommandPort.existsByWalletIdAndCommandId(walletId, commandId);
    }

    public Mono<Void> storeAndSendExecutedCommand(Wallet wallet) {
        return storeNotSendExecutedCommand(wallet).flatMap(this::sendWalletUpdateEvent);
    }

    private Mono<ExecutedCommand> storeNotSendExecutedCommand(Wallet wallet) {
        log.info("Storing executed command for wallet: {} and last executed command: {}",
                wallet.getId(), wallet.getLastExecutedCommandId());
        return executedCommandPort.save(ExecutedCommand.fromLastExecutedCommand(wallet));
    }

    private Mono<Void> sendWalletUpdateEvent(ExecutedCommand command) {
        //Checking if any other command is not send for wallet to prevent from sending wallet events in wrong order
        return executedCommandPort.existsByWalletIdAndCommandIdNotAndSend(command.getWalletId(), command.getCommandId(), false)
                .flatMap(notSendCommandExists -> {
                    if (notSendCommandExists) {
                        log.info("Not sending wallet update event for wallet: {}, other not send commands exists", command.getWalletId());
                        return Mono.empty();
                    } else {
                        return walletEventsPort.sendEventFromExecutedCommand(command)
                                .then(markExecutedCommandAsSend(command))
                                .onErrorComplete(); //ignoring errors since executed command will go through outbox pattern flow if sending failed
                    }
                });
    }

    private Mono<Void> markExecutedCommandAsSend(ExecutedCommand executedCommand) {
        //this could be done using atomic update in mongo, yet I find this a cleaner way
        log.info("Marking executed command as send: {}", executedCommand.getId());
        executedCommand.markAsSend();
        return executedCommandPort.save(executedCommand).then();
    }
}
