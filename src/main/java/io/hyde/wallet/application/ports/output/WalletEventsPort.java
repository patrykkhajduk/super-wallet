package io.hyde.wallet.application.ports.output;

import io.hyde.wallet.domain.model.Wallet;
import io.hyde.wallet.domain.model.command.result.WalletCommandResult;
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.WalletEvent;
import reactor.core.publisher.Mono;

public interface WalletEventsPort {

    Mono<Void> sendEventFromCommandExecutionResult(Wallet wallet, WalletCommandResult result);

    Mono<Void> sendErrorEvent(String commandId, String walletId, String errorMessage);
}
