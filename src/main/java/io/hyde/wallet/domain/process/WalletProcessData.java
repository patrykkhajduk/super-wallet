package io.hyde.wallet.domain.process;

import io.hyde.wallet.domain.model.Wallet;
import io.hyde.wallet.domain.model.process.WalletProcess;

public record WalletProcessData(Wallet wallet, WalletProcess process) {
}
