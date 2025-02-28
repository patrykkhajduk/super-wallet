package io.hyde.wallet.utils.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationConsts {

    public static final int MAX_ID_LENGTH = 128;
    public static final String ID_REGEXP = "[a-zA-Z0-9-]*";
    public static final int MAX_TOKEN_LENGTH = 32;
    public static final String TOKEN_REGEXP = "[a-zA-Z0-9]*";
}
