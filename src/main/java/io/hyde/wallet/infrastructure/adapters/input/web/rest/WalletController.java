package io.hyde.wallet.infrastructure.adapters.input.web.rest;

import io.hyde.wallet.application.ports.input.CreateWalletUseCase;
import io.hyde.wallet.application.ports.input.FindWalletsUseCase;
import io.hyde.wallet.infrastructure.adapters.input.web.rest.request.CreateWalletRequest;
import io.hyde.wallet.infrastructure.adapters.input.web.rest.response.WalletDto;
import io.hyde.wallet.utils.validation.ValidationConsts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static io.hyde.wallet.infrastructure.adapters.input.web.rest.WalletController.BASE_URL;

@RestController
@RequestMapping(BASE_URL)
@RequiredArgsConstructor
@Validated
class WalletController {

    public static final String BASE_URL = "/api/v1/wallets";

    private final FindWalletsUseCase findWalletsUseCase;
    private final CreateWalletUseCase createWalletUseCase;

    @GetMapping
    public Mono<Page<WalletDto>> findAllWallets(@RequestParam(value = "pageNumber", defaultValue = "0")
                                                @PositiveOrZero(message = "Page number must be positive or zero")
                                                int pageNumber,
                                                @RequestParam(value = "pageSize", defaultValue = "10")
                                                @Positive(message = "Page size must be positive number")
                                                @Max(value = 100, message = "Page size must be less than or equal to {max}")
                                                int pageSize) {
        return findWalletsUseCase.findAllWallets(PageRequest.of(pageNumber, pageSize))
                .map(page -> page.map(WalletDto::from));
    }

    @GetMapping("/{walletId}")
    public Mono<ResponseEntity<WalletDto>> findWallet(@PathVariable
                                                      @Length(max = ValidationConsts.MAX_ID_LENGTH, message = "Wallet id cannot be longer than {max} characters")
                                                      @Pattern(regexp = ValidationConsts.ID_REGEXP, message = "Invalid wallet id format")
                                                      String walletId) {
        return findWalletsUseCase.findWallet(walletId)
                .map(WalletDto::from)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<WalletDto> createWallet(@RequestBody @Valid CreateWalletRequest request) {
        return createWalletUseCase.createWallet(request.toDto())
                .map(WalletDto::from);
    }
}
