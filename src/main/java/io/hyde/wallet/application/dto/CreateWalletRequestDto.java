package io.hyde.wallet.application.dto;

import io.hyde.wallet.domain.model.Wallet;

public record CreateWalletRequestDto(String ownerId) {

    public Wallet toWallet() {
        return Wallet.forOwner(ownerId);
    }
}
