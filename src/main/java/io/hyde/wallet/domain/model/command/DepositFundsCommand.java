package io.hyde.wallet.domain.model.command;

import java.math.BigDecimal;

public record DepositFundsCommand(String id,
                                  String walletId,
                                  String token,
                                  BigDecimal amount) implements WalletTokenRelatedCommand {
}