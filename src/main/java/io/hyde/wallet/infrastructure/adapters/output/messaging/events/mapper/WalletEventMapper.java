package io.hyde.wallet.infrastructure.adapters.output.messaging.events.mapper;

import io.hyde.wallet.domain.model.Wallet;
import io.hyde.wallet.domain.model.command.result.BlockFundsCommandResult;
import io.hyde.wallet.domain.model.command.result.DepositFundsCommandResult;
import io.hyde.wallet.domain.model.command.result.ReleaseFundsCommandResult;
import io.hyde.wallet.domain.model.command.result.WalletCommandResult;
import io.hyde.wallet.domain.model.command.result.WithdrawFundsCommandResult;
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.FundsAddedEvent;
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.FundsBlockedEvent;
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.FundsReleasedEvent;
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.FundsWithdrawnEvent;
import io.hyde.wallet.infrastructure.adapters.output.messaging.events.WalletEvent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WalletEventMapper {

    public static WalletEvent map(WalletCommandResult result, Wallet walletSnapshot) {
        return switch (result) {
            case DepositFundsCommandResult command -> FundsAddedEvent.from(command, walletSnapshot);
            case BlockFundsCommandResult command -> FundsBlockedEvent.from(command, walletSnapshot);
            case ReleaseFundsCommandResult command -> FundsReleasedEvent.from(command, walletSnapshot);
            case WithdrawFundsCommandResult command -> FundsWithdrawnEvent.from(command, walletSnapshot);
        };
    }
}
