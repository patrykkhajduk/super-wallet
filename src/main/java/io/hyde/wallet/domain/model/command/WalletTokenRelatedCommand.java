package io.hyde.wallet.domain.model.command;

public sealed interface WalletTokenRelatedCommand extends WalletCommand permits DepositFundsCommand, BlockFundsCommand {

    String getToken();

}
