package io.hyde.wallet.application.ports.input;

import io.hyde.wallet.domain.model.Token;
import reactor.core.publisher.Flux;

public interface FindTokensUseCase {

    Flux<Token> findAllTokens();
}
