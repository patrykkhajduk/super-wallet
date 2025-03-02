package io.hyde.wallet.domain.model.command;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

import java.math.BigDecimal;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
@Getter
@TypeAlias("blockFundsCommand")
public final class BlockFundsCommand implements WalletTokenRelatedCommand {

    private String id;
    private String walletId;
    private String token;
    private BigDecimal amount;
}
