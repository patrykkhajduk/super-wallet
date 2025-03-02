package io.hyde.wallet.domain.service;

import io.hyde.wallet.application.dto.CreateWalletRequestDto;
import io.hyde.wallet.application.ports.input.CreateWalletUseCase;
import io.hyde.wallet.application.ports.input.FindWalletsUseCase;
import io.hyde.wallet.application.ports.input.UpdateWalletUseCase;
import io.hyde.wallet.application.ports.output.WalletPort;
import io.hyde.wallet.domain.exception.ValidationException;
import io.hyde.wallet.domain.model.Wallet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
class WalletService implements FindWalletsUseCase, CreateWalletUseCase, UpdateWalletUseCase {

    private final int walletsLimit;
    private final WalletPort walletPort;

    public WalletService(@Value("${wallets.limit-per-owner}") Integer walletsLimit,
                         WalletPort walletPort) {
        this.walletsLimit = walletsLimit;
        this.walletPort = walletPort;
    }

    @Override
    public Mono<Page<Wallet>> findAllWallets(Pageable pageable) {
        return walletPort.findAllBy(pageable)
                .collectList()
                .zipWith(walletPort.count())
                .map(pair -> new PageImpl<>(pair.getT1(), pageable, pair.getT2()));
    }

    @Override
    public Mono<Wallet> findWallet(String id) {
        return walletPort.findById(id);
    }

    @Override
    public Mono<Wallet> createWallet(CreateWalletRequestDto request) {
        log.info("Creating wallet for owner: {}", request.ownerId());
        return walletPort.countByOwnerId(request.ownerId())
                .filter(count -> count < walletsLimit)
                .switchIfEmpty(Mono.error(new ValidationException("Wallets limit reached for owner: " + request.ownerId())))
                .then(walletPort.save(request.toWallet()))
                .doOnSuccess(wallet -> log.info("Wallet created: {}", wallet.getId()));
    }


    @Override
    public Mono<Wallet> updateWallet(Wallet wallet) {
        log.info("Updating wallet: {}", wallet.getId());
        return walletPort.save(wallet);
    }
}
