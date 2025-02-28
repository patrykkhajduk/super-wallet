package io.hyde.wallet.application.ports.output;

import io.hyde.wallet.domain.model.Wallet;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface WalletPort {

    Mono<Wallet> findById(String id);

    Mono<Long> countByOwnerId(String ownerId);

    Flux<Wallet> findAllBy(Pageable pageable);

    Mono<Long> count();

    <W extends Wallet> Mono<W> save(W wallet);

    Flux<Wallet> findAllWithoutStoredLastExecutedCommand(LocalDateTime lastModifiedDateBefore);
}
