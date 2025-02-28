package io.hyde.wallet.application.ports.input;

import io.hyde.wallet.domain.model.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

public interface FindWalletsUseCase {

    Mono<Page<Wallet>> findAllWallets(Pageable pageable);

    Mono<Wallet> findWallet(String id);
}
