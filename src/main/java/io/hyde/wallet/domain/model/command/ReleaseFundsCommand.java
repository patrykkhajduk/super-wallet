package io.hyde.wallet.domain.model.command;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor
@Getter
@TypeAlias("releaseFundsCommand")
public final class ReleaseFundsCommand implements WalletCommand {

    private String id;
    private String walletId;
    private String lockId;
}
