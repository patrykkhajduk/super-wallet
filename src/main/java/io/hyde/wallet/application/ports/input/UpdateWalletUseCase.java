package io.hyde.wallet.application.ports.input;

import io.hyde.wallet.application.dto.CreateWalletRequestDto;
import io.hyde.wallet.domain.model.Wallet;
import reactor.core.publisher.Mono;

public interface UpdateWalletUseCase {

    Mono<Wallet> updateWallet(Wallet wallet);
}
