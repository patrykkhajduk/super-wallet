package io.hyde.wallet.application.ports.input;

import io.hyde.wallet.application.dto.CreateWalletRequestDto;
import io.hyde.wallet.domain.model.Wallet;
import reactor.core.publisher.Mono;

public interface CreateWalletUseCase {

    Mono<Wallet> createWallet(CreateWalletRequestDto request);
}
