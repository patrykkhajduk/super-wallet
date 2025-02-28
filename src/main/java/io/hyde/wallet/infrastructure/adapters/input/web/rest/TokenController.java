package io.hyde.wallet.infrastructure.adapters.input.web.rest;

import io.hyde.wallet.application.ports.input.CreateTokenUseCase;
import io.hyde.wallet.application.ports.input.FindTokensUseCase;
import io.hyde.wallet.infrastructure.adapters.input.web.rest.request.CreateTokenRequest;
import io.hyde.wallet.infrastructure.adapters.input.web.rest.response.TokenDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static io.hyde.wallet.infrastructure.adapters.input.web.rest.TokenController.BASE_URL;

@RestController
@RequestMapping(BASE_URL)
@RequiredArgsConstructor
@Validated
class TokenController {

    public static final String BASE_URL = "/api/v1/tokens";

    private final FindTokensUseCase findTokensUseCase;
    private final CreateTokenUseCase createTokenUseCase;

    @GetMapping(produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<TokenDto> findAllTokens() {
        return findTokensUseCase.findAllTokens()
                .map(TokenDto::from);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TokenDto> createWallet(@RequestBody @Valid CreateTokenRequest request) {
        return createTokenUseCase.createToken(request.toDto())
                .map(TokenDto::from);
    }
}
