package io.hyde.wallet.domain.validator;

import io.hyde.wallet.application.ports.output.TokenPort;
import io.hyde.wallet.domain.exception.WalletCommandProcessingException;
import io.hyde.wallet.domain.model.command.WalletCommand;
import io.hyde.wallet.domain.model.command.WalletTokenRelatedCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalletCommandValidator {

    private final TokenPort tokenPort;

    public Mono<Void> validate(WalletCommand command) {
        return switch (command) {
            case WalletTokenRelatedCommand walletTokenRelatedCommand ->
                    validateToken(walletTokenRelatedCommand.getToken());
            default -> Mono.empty();
        };
    }

    private Mono<Void> validateToken(String token) {
        return tokenPort.existsByName(token)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new WalletCommandProcessingException("Token not found: " + token)))
                .then();
    }
}
