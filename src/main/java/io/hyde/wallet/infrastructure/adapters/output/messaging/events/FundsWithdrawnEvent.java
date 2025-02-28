package io.hyde.wallet.infrastructure.adapters.output.messaging.events;

import io.hyde.wallet.domain.model.Wallet;
import io.hyde.wallet.domain.model.command.result.WithdrawFundsCommandResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FundsWithdrawnEvent(String id,
                                  String lockId,
                                  String withdrawnFundsToken,
                                  BigDecimal withdrawnFundsAmount,
                                  WalletSnapshot wallet,
                                  LocalDateTime updatedAt) implements WalletEvent {

    public static FundsWithdrawnEvent from(WithdrawFundsCommandResult result, Wallet wallet) {
        return new FundsWithdrawnEvent(
                result.getId(),
                result.getLockId(),
                result.getWithdrawnFundsToken(),
                result.getWithdrawnFundsAmount(),
                WalletSnapshot.from(wallet),
                result.getTimestamp());
    }

    @Override
    public String walletId() {
        return wallet.id();
    }
}
