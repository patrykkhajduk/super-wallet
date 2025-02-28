package io.hyde.wallet.infrastructure.adapters.input.web.rest.response;

import io.hyde.wallet.domain.model.Wallet;
import io.hyde.wallet.domain.model.Wallet.Fund;

import java.math.BigDecimal;
import java.util.List;

public record WalletDto(String id,
                        String ownerId,
                        List<FundDto> balance) {

    public static WalletDto from(Wallet wallet) {
        return new WalletDto(
                wallet.getId(),
                wallet.getOwnerId(),
                wallet.getFunds()
                        .entrySet()
                        .stream()
                        .map(e -> FundDto.from(e.getKey(), e.getValue()))
                        .toList());
    }

    public record FundDto(String token, BigDecimal available, BigDecimal blocked) {

        public static FundDto from(String token, Fund fund) {
            return new FundDto(token, fund.getAvailable(), fund.getTotalBlocked());
        }
    }
}
