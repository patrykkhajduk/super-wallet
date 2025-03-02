package io.hyde.wallet.infrastructure.adapters.output.persistence.repository;

import io.hyde.wallet.application.ports.output.WalletProcessPort;
import io.hyde.wallet.domain.model.process.WalletProcess;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface WalletProcessRepository extends WalletProcessPort, ReactiveCrudRepository<WalletProcess, String> {
}
