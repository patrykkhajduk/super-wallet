package io.hyde.wallet.infrastructure.adapters.output.messaging.events;

import io.hyde.wallet.domain.model.Wallet;
import io.hyde.wallet.domain.model.Wallet.Fund;

import java.math.BigDecimal;
import java.util.List;

public record WalletSnapshot(String id,
                             String ownerId,
                             List<FundSnapshot> balance) {

    public static WalletSnapshot from(Wallet wallet) {
        return new WalletSnapshot(
                wallet.getId(),
                wallet.getOwnerId(),
                wallet.getFunds()
                        .entrySet()
                        .stream()
                        .map(e -> FundSnapshot.from(e.getKey(), e.getValue()))
                        .toList());
    }

    public record FundSnapshot(String token, BigDecimal available, BigDecimal blocked) {

        public static FundSnapshot from(String token, Fund fund) {
            return new FundSnapshot(token, fund.getAvailable(), fund.getTotalBlocked());
        }
    }
}
