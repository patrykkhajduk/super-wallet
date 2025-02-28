package io.hyde.wallet.domain.model.command.result;

import java.time.LocalDateTime;

public sealed interface WalletCommandResult permits DepositFundsCommandResult, BlockFundsCommandResult, WithdrawFundsCommandResult, ReleaseFundsCommandResult {

    String getId();

    String getWalletId();

    LocalDateTime getTimestamp();
}
