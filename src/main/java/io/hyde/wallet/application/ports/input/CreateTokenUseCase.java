package io.hyde.wallet.application.ports.input;

import io.hyde.wallet.application.dto.CreateTokenRequestDto;
import io.hyde.wallet.domain.model.Token;
import reactor.core.publisher.Mono;

public interface CreateTokenUseCase {

    Mono<Token> createToken(CreateTokenRequestDto request);
}
