package io.hyde.wallet.domain.model.command.result;

import io.hyde.wallet.domain.model.command.ReleaseFundsCommand;
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
@TypeAlias("releaseFundsCommandResult")
public final class ReleaseFundsCommandResult implements WalletCommandResult {

    public static ReleaseFundsCommandResult from(ReleaseFundsCommand command,
                                                 String releasedFundsToken,
                                                 BigDecimal releasedFundsAmount,
                                                 LocalDateTime timestamp) {
        return new ReleaseFundsCommandResult(
                command.getId(), command.getWalletId(), command.getLockId(), releasedFundsToken, releasedFundsAmount, timestamp);
    }

    private String id;
    private String walletId;
    private String lockId;
    private String releasedFundsToken;
    private BigDecimal releasedFundsAmount;
    private LocalDateTime timestamp;
}
