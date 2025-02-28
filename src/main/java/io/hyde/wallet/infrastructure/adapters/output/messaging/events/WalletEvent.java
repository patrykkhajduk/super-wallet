package io.hyde.wallet.infrastructure.adapters.output.messaging.events;

public sealed interface WalletEvent permits FundsAddedEvent, FundsBlockedEvent, FundsReleasedEvent, FundsWithdrawnEvent, ErrorEvent {

    String walletId();
}
