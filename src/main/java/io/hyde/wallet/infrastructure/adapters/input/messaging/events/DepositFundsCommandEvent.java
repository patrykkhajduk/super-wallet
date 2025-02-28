package io.hyde.wallet.infrastructure.adapters.input.messaging.events;

import io.hyde.wallet.utils.validation.ValidationConsts;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

public record DepositFundsCommandEvent(@NotBlank(message = "Id is required")
                                       @Length(max = ValidationConsts.MAX_ID_LENGTH, message = "Id cannot be longer than {max} characters")
                                       @Pattern(regexp = ValidationConsts.ID_REGEXP, message = "Invalid id format")
                                       String id,
                                       @NotBlank(message = "Wallet id is required")
                                       @Length(max = ValidationConsts.MAX_ID_LENGTH, message = "Wallet id cannot be longer than {max} characters")
                                       @Pattern(regexp = ValidationConsts.ID_REGEXP, message = "Invalid wallet id format")
                                       String walletId,
                                       @NotBlank(message = "Token is required")
                                       @Length(max = ValidationConsts.MAX_TOKEN_LENGTH, message = "Token cannot be longer than {max} characters")
                                       @Pattern(regexp = ValidationConsts.TOKEN_REGEXP, message = "Invalid token format")
                                       String token,
                                       @NotNull(message = "Amount is required")
                                       @Positive(message = "Amount must be greater than zero")
                                       BigDecimal amount) implements WalletCommandEvent {
}