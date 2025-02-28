package io.hyde.wallet.domain.model.command;

public record WithdrawFundsCommand(String id,
                                   String walletId,
                                   String lockId) implements WalletCommand {
}
