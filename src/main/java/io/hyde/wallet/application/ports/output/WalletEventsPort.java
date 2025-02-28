package io.hyde.wallet.application.ports.output;

import io.hyde.wallet.domain.model.ExecutedCommand;
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.WalletEvent;
import reactor.core.publisher.Mono;

public interface WalletEventsPort {

    Mono<Void> sendEventFromExecutedCommand(ExecutedCommand executedCommand);

    Mono<Void> sendEvent(WalletEvent event);
}
