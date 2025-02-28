package io.hyde.wallet.infrastructure.adapters.output.messaging.events;

import io.hyde.wallet.domain.model.Wallet;
import io.hyde.wallet.domain.model.command.result.DepositFundsCommandResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FundsAddedEvent(String id,
                              String token,
                              BigDecimal amount,
                              WalletSnapshot wallet,
                              LocalDateTime updatedAt) implements WalletEvent {

    public static FundsAddedEvent from(DepositFundsCommandResult result, Wallet wallet) {
        return new FundsAddedEvent(
                result.getId(),
                result.getToken(),
                result.getAmount(),
                WalletSnapshot.from(wallet),
                result.getTimestamp());
    }

    @Override
    public String walletId() {
        return wallet.id();
    }
}