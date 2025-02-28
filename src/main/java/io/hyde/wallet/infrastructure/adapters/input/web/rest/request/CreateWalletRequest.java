package io.hyde.wallet.infrastructure.adapters.input.web.rest.request;

import io.hyde.wallet.application.dto.CreateWalletRequestDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record CreateWalletRequest(@NotBlank(message = "OwnerId is required")
                                  @Length(max = 128, message = "OwnerId must be less than or equal to {max} characters")
                                  @Pattern(regexp = "[a-zA-Z0-9-]*", message = "OwnerId has invalid characters")
                                  String ownerId) {

    public CreateWalletRequestDto toDto() {
        return new CreateWalletRequestDto(ownerId);
    }
}
