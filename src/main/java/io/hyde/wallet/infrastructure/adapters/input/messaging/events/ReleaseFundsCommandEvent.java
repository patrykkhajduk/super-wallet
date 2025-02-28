package io.hyde.wallet.infrastructure.adapters.input.messaging.events;

import io.hyde.wallet.utils.validation.ValidationConsts;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record ReleaseFundsCommandEvent(@NotBlank(message = "Id is required")
                                       @Length(max = ValidationConsts.MAX_ID_LENGTH, message = "Id cannot be longer than {max} characters")
                                       @Pattern(regexp = ValidationConsts.ID_REGEXP, message = "Invalid id format")
                                       String id,
                                       @NotBlank(message = "Wallet id is required")
                                       @Length(max = ValidationConsts.MAX_ID_LENGTH, message = "Wallet id cannot be longer than {max} characters")
                                       @Pattern(regexp = ValidationConsts.ID_REGEXP, message = "Invalid wallet id format")
                                       String walletId,
                                       @NotBlank(message = "Lock id is required")
                                       @Length(max = ValidationConsts.MAX_ID_LENGTH, message = "Lock id cannot be longer than {max} characters")
                                       @Pattern(regexp = ValidationConsts.ID_REGEXP, message = "Invalid lock id format")
                                       String lockId) implements WalletCommandEvent {
}
