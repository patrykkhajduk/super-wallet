package io.hyde.wallet.infrastructure.adapters.output.persistence.repository;

import io.hyde.wallet.application.ports.output.WalletPort;
import io.hyde.wallet.domain.model.Wallet;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

public interface WalletRepository extends WalletPort, ReactiveCrudRepository<Wallet, String> {

    @Aggregation(pipeline = {
            """
            {
                "$lookup":
                {
                    "from": "executedCommand",
                    "localField": "lastExecutedCommandResult._id",
                    "foreignField": "commandId",
                    "as": "commands"
                }
            }
            """,
            """
            {
                "$match":
                {
                    "lastModifiedDate": { $lt : ?0 },
                    "lastExecutedCommandResult._id": { $exists : true },
                    "commands": { $size : 0 }
                }
            }
            """,
            """
            {
                "$project":
                {
                    "commands": 0
                }
            }
            """})
    Flux<Wallet> findAllWithoutStoredLastExecutedCommand(LocalDateTime lastModifiedDateBefore);
}
