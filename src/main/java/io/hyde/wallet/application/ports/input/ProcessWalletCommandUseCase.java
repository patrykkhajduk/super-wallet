package io.hyde.wallet.application.ports.input;

import io.hyde.wallet.domain.model.command.WalletCommand;
import reactor.core.publisher.Mono;

public interface ProcessWalletCommandUseCase {

    Mono<Void> process(WalletCommand command);
}
