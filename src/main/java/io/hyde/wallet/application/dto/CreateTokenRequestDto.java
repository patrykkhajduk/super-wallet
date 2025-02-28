package io.hyde.wallet.application.dto;

import io.hyde.wallet.domain.model.Token;

public record CreateTokenRequestDto(String name) {

    public Token toToken() {
        return Token.ofName(name);
    }
}
