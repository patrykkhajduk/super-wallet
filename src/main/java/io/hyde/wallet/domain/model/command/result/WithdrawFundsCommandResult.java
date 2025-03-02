package io.hyde.wallet.domain.model.command.result;

import io.hyde.wallet.domain.model.command.WithdrawFundsCommand;
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
@TypeAlias("withdrawFundsCommandResult")
public final class WithdrawFundsCommandResult implements WalletCommandResult {

    public static WithdrawFundsCommandResult from(WithdrawFundsCommand command,
                                                  String withdrawnFundsToken,
                                                  BigDecimal withdrawnFundsAmount,
                                                  LocalDateTime timestamp) {
        return new WithdrawFundsCommandResult(
                command.getId(), command.getWalletId(), command.getLockId(), withdrawnFundsToken, withdrawnFundsAmount, timestamp);
    }

    private String id;
    private String walletId;
    private String lockId;
    private String withdrawnFundsToken;
    private BigDecimal withdrawnFundsAmount;
    private LocalDateTime timestamp;
}
