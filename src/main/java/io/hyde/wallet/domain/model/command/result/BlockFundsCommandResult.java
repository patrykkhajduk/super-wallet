package io.hyde.wallet.domain.model.command.result;

import io.hyde.wallet.domain.model.command.BlockFundsCommand;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@TypeAlias("blockFundsCommandResult")
public final class BlockFundsCommandResult implements WalletCommandResult {

    public static BlockFundsCommandResult from(BlockFundsCommand command, String lockId, LocalDateTime timestamp) {
        return new BlockFundsCommandResult(
                command.id(), command.walletId(), command.token(), command.amount(), lockId, timestamp);
    }

    private String id;
    private String walletId;
    private String token;
    private BigDecimal amount;
    private String blockedFundsLockId;
    private LocalDateTime timestamp;
}
