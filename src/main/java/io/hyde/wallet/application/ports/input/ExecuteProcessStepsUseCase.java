package io.hyde.wallet.application.ports.input;

import io.hyde.wallet.domain.model.process.WalletProcess;
import reactor.core.publisher.Mono;

public interface ExecuteProcessStepsUseCase {

    Mono<Void> processSteps(WalletProcess process);
}
