package io.hyde.wallet.domain.service;

import io.hyde.wallet.application.dto.CreateTokenRequestDto;
import io.hyde.wallet.application.ports.input.CreateTokenUseCase;
import io.hyde.wallet.application.ports.input.FindTokensUseCase;
import io.hyde.wallet.application.ports.output.TokenPort;
import io.hyde.wallet.domain.exception.ValidationException;
import io.hyde.wallet.domain.model.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
class TokenService implements FindTokensUseCase, CreateTokenUseCase {

    private final TokenPort tokenPort;

    @Override
    public Flux<Token> findAllTokens() {
        return tokenPort.findAll();
    }

    @Override
    public Mono<Token> createToken(CreateTokenRequestDto request) {
        return tokenPort.existsByName(request.name())
                .filter(Predicate.not(Boolean::booleanValue))
                .switchIfEmpty(Mono.error(new ValidationException("Token with name %s already exists".formatted(request.name()))))
                .then(tokenPort.save(request.toToken()))
                .doOnSuccess(token -> log.info("Token created: {}", token.getId()));
    }
}
