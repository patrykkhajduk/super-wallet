package io.hyde.wallet.infrastructure.adapters.output.messaging.events;

import io.hyde.wallet.domain.model.Wallet;
import io.hyde.wallet.domain.model.command.result.BlockFundsCommandResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FundsBlockedEvent(String id,
                                String token,
                                BigDecimal amount,
                                String blockedFundsLockId,
                                WalletSnapshot wallet,
                                LocalDateTime updatedAt) implements WalletEvent {

    public static FundsBlockedEvent from(BlockFundsCommandResult result, Wallet wallet) {
        return new FundsBlockedEvent(
                result.getId(),
                result.getToken(),
                result.getAmount(),
                result.getBlockedFundsLockId(),
                WalletSnapshot.from(wallet),
                result.getTimestamp());
    }

    @Override
    public String walletId() {
        return wallet.id();
    }
}
