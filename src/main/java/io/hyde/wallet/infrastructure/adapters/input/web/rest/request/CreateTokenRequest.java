package io.hyde.wallet.infrastructure.adapters.input.web.rest.request;

import io.hyde.wallet.application.dto.CreateTokenRequestDto;
import io.hyde.wallet.application.dto.CreateWalletRequestDto;
import io.hyde.wallet.utils.validation.ValidationConsts;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record CreateTokenRequest(@NotBlank(message = "Name is required")
                                 @Length(max = ValidationConsts.MAX_TOKEN_LENGTH, message = "Name cannot be longer than {max} characters")
                                 @Pattern(regexp = ValidationConsts.TOKEN_REGEXP, message = "Invalid name format")
                                 String name) {

    public CreateTokenRequestDto toDto() {
        return new CreateTokenRequestDto(name);
    }
}
