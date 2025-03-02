package io.hyde.wallet.domain.process.step;

import com.google.common.base.Preconditions;
import io.hyde.wallet.application.ports.output.WalletEventsPort;
import io.hyde.wallet.domain.model.process.WalletProcess.WalletProcessStep;
import io.hyde.wallet.domain.process.WalletProcessData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletSendResponseStepProcessor implements WalletStepProcessor {

    private final WalletEventsPort walletEventsPort;


    @Override
    public WalletProcessStep getStep() {
        return WalletProcessStep.SEND_RESPONSE;
    }

    @Override
    public Mono<WalletProcessData> executeStep(WalletProcessData data) {
        Preconditions.checkNotNull(data.process().getWalletCommandResult(),
                "Wallet command result is missing in process: " + data.process().getId());
        return walletEventsPort.sendEventFromCommandExecutionResult(data.wallet(), data.process().getWalletCommandResult())
                .then(Mono.just(data));
    }
}
