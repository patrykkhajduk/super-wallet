package io.hyde.wallet.domain.model.command.result;

import io.hyde.wallet.domain.model.command.DepositFundsCommand;
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
@TypeAlias("depositFundsCommandResult")
public final class DepositFundsCommandResult implements WalletCommandResult {

    public static DepositFundsCommandResult from(DepositFundsCommand command, LocalDateTime timestamp) {
        return new DepositFundsCommandResult(
                command.id(), command.walletId(), command.token(), command.amount(), timestamp);
    }

    private String id;
    private String walletId;
    private String token;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}