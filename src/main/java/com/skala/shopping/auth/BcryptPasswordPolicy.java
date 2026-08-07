package com.skala.shopping.auth;

import java.nio.charset.StandardCharsets;

public final class BcryptPasswordPolicy {

    public static final int MAX_UTF8_BYTES = 72;
    public static final String VALIDATION_MESSAGE =
            "비밀번호는 UTF-8 기준 72바이트 이하여야 합니다.";

    private BcryptPasswordPolicy() {
    }

    public static boolean isCompatible(CharSequence rawPassword) {
        return rawPassword != null
                && rawPassword.toString().getBytes(StandardCharsets.UTF_8).length
                <= MAX_UTF8_BYTES;
    }
}
