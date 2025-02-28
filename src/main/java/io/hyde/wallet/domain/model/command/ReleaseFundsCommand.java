package io.hyde.wallet.domain.model.command;

public record ReleaseFundsCommand(String id,
                                  String walletId,
                                  String lockId) implements WalletCommand {
}
