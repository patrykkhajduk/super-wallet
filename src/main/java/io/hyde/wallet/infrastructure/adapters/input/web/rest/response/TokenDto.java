package io.hyde.wallet.infrastructure.adapters.input.web.rest.response;

import io.hyde.wallet.domain.model.Token;

public record TokenDto(String name) {

    public static TokenDto from(Token token) {
        return new TokenDto(token.getName());
    }
}
