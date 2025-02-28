package io.hyde.wallet.infrastructure.adapters.input.messaging.events.mapper;

import io.hyde.wallet.domain.model.command.BlockFundsCommand;
import io.hyde.wallet.domain.model.command.DepositFundsCommand;
import io.hyde.wallet.domain.model.command.ReleaseFundsCommand;
import io.hyde.wallet.domain.model.command.WalletCommand;
import io.hyde.wallet.domain.model.command.WithdrawFundsCommand;
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.BlockFundsCommandEvent;
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.DepositFundsCommandEvent;
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.ReleaseFundsCommandEvent;
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.WalletCommandEvent;
import io.hyde.wallet.infrastructure.adapters.input.messaging.events.WithdrawFundsCommandEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WalletCommandEventMapper {

    public static WalletCommand map(WalletCommandEvent event) {
        return switch (event) {
            case DepositFundsCommandEvent command -> map(command);
            case BlockFundsCommandEvent command -> map(command);
            case ReleaseFundsCommandEvent command -> map(command);
            case WithdrawFundsCommandEvent command -> map(command);
        };
    }

    private static DepositFundsCommand map(DepositFundsCommandEvent event) {
        return new DepositFundsCommand(event.id(), event.walletId(), event.token(), event.amount());
    }

    private static BlockFundsCommand map(BlockFundsCommandEvent event) {
        return new BlockFundsCommand(event.id(), event.walletId(), event.token(), event.amount());
    }

    private static ReleaseFundsCommand map(ReleaseFundsCommandEvent event) {
        return new ReleaseFundsCommand(event.id(), event.walletId(), event.lockId());
    }

    private static WithdrawFundsCommand map(WithdrawFundsCommandEvent event) {
        return new WithdrawFundsCommand(event.id(), event.walletId(), event.lockId());
    }
}
