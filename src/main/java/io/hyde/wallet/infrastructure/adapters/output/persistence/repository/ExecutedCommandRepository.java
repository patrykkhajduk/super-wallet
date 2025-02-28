package io.hyde.wallet.infrastructure.adapters.output.persistence.repository;

import io.hyde.wallet.application.ports.output.ExecutedCommandPort;
import io.hyde.wallet.domain.model.ExecutedCommand;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ExecutedCommandRepository extends ExecutedCommandPort, ReactiveCrudRepository<ExecutedCommand, String> {

    Mono<ExecutedCommand> findByWalletIdAndCommandId(String walletId, String commandId);
}
