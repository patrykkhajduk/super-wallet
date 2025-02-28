package io.hyde.wallet.application.ports.output;

import io.hyde.wallet.domain.model.ExecutedCommand;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface ExecutedCommandPort {

    Mono<Boolean> existsByWalletIdAndCommandId(String walletId, String commandId);

    Mono<Boolean> existsByWalletIdAndCommandIdNotAndSend(String walletId, String commandId, boolean send);

    Flux<ExecutedCommand> findAllBySendAndCreatedDateLessThanOrderByCreatedDateAsc(
            boolean send, LocalDateTime localDateTime, Pageable pageable);

    <C extends ExecutedCommand> Mono<C> save(C command);
}
