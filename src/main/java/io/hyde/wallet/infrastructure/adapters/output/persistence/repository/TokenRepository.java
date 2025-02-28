package io.hyde.wallet.infrastructure.adapters.output.persistence.repository;

import io.hyde.wallet.application.ports.output.TokenPort;
import io.hyde.wallet.domain.model.Token;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface TokenRepository extends TokenPort, ReactiveCrudRepository<Token, String> {

    Mono<Boolean> existsByName(String name);
}
