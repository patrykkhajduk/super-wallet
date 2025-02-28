package io.hyde.wallet.infrastructure.adapters.input.messaging.events;

public sealed interface WalletCommandEvent permits DepositFundsCommandEvent, BlockFundsCommandEvent, ReleaseFundsCommandEvent, WithdrawFundsCommandEvent {

    String id();

    String walletId();
}
