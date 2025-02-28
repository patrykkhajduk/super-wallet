package io.hyde.wallet.infrastructure.adapters.output.messaging.events;

import io.hyde.wallet.domain.exception.ApplicationException;
import io.hyde.wallet.domain.model.command.WalletCommand;

public record ErrorEvent(String commandId,
                         String walletId,
                         String errorMessage) implements WalletEvent {

    public static ErrorEvent from(WalletCommand event, ApplicationException exception) {
        return new ErrorEvent(
                event.id(),
                event.walletId(),
                exception.getMessage());
    }
}
