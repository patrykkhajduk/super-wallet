package io.hyde.wallet.domain.process.step;

import io.hyde.wallet.domain.model.Wallet;
import io.hyde.wallet.domain.model.process.WalletProcess;
import io.hyde.wallet.domain.model.process.WalletProcess.WalletProcessStep;
import io.hyde.wallet.domain.process.WalletProcessData;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

public interface WalletStepProcessor {

    WalletProcessStep getStep();

    Mono<WalletProcessData> executeStep(WalletProcessData processData);
}
