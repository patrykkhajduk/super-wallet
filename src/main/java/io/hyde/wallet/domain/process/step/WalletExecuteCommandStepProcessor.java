package io.hyde.wallet.domain.process.step;

import com.google.common.base.Preconditions;
import io.hyde.wallet.domain.model.process.WalletProcess.WalletProcessStep;
import io.hyde.wallet.domain.process.WalletProcessData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Clock;

@Component
@RequiredArgsConstructor
public class WalletExecuteCommandStepProcessor implements WalletStepProcessor {

    private final Clock clock;

    @Override
    public WalletProcessStep getStep() {
        return WalletProcessStep.EXECUTE_COMMAND;
    }

    @Override
    public Mono<WalletProcessData> executeStep(WalletProcessData data) {
        Preconditions.checkNotNull(data.process().getWalletCommand(),
                "Wallet command is missing in process: " + data.process().getId());
        data.process().setWalletCommandResult(data.wallet().execute(data.process().getWalletCommand(), clock));
        return Mono.just(data);
    }
}
