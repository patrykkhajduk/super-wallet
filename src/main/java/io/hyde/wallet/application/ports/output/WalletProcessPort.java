package io.hyde.wallet.application.ports.output;

import io.hyde.wallet.domain.model.process.WalletProcess;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface WalletProcessPort {

    Mono<Boolean> existsByCommandIdAndWalletIdAndCompleted(String commandId, String walletId, boolean completed);

    Mono<Boolean> existsByWalletIdAndCompleted(String walletId, boolean completed);

    Mono<WalletProcess> findByCommandIdAndWalletId(String commandId, String walletId);

    Flux<WalletProcess> findAllByCompletedAndCreatedDateLessThanOrderByCreatedDateAsc(boolean completed, LocalDateTime localDateTime);

    <W extends WalletProcess> Mono<W> save(W walletProcess);
}
