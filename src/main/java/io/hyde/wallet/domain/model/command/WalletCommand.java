package io.hyde.wallet.domain.model.command;

public sealed interface WalletCommand permits WalletTokenRelatedCommand, WithdrawFundsCommand, ReleaseFundsCommand {

    String getId();

    String getWalletId();
}
