package io.hyde.wallet.infrastructure.adapters.output.messaging.events;

import io.hyde.wallet.domain.model.Wallet;
import io.hyde.wallet.domain.model.command.result.ReleaseFundsCommandResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FundsReleasedEvent(String id,
                                 String lockId,
                                 String releasedFundsToken,
                                 BigDecimal releasedFundsAmount,
                                 WalletSnapshot wallet,
                                 LocalDateTime updatedAt) implements WalletEvent {

    public static FundsReleasedEvent from(ReleaseFundsCommandResult result, Wallet wallet) {
        return new FundsReleasedEvent(
                result.getId(),
                result.getLockId(),
                result.getReleasedFundsToken(),
                result.getReleasedFundsAmount(),
                WalletSnapshot.from(wallet),
                result.getTimestamp());
    }

    @Override
    public String walletId() {
        return wallet.id();
    }
}
