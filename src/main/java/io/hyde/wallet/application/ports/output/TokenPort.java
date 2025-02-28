package io.hyde.wallet.application.ports.output;

import io.hyde.wallet.domain.model.Token;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TokenPort {

    Flux<Token> findAll();

    <T extends Token> Mono<T> save(T token);

    Mono<Boolean> existsByName(String name);
}
