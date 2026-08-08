package com.skala.shopping.auth.internal;

import java.nio.charset.StandardCharsets; import java.security.*; import java.util.Base64; import java.util.HexFormat;

final class RefreshTokenSupport {
    private static final SecureRandom RANDOM=new SecureRandom(); private RefreshTokenSupport(){}
    static String generate(){byte[] value=new byte[32];RANDOM.nextBytes(value);return Base64.getUrlEncoder().withoutPadding().encodeToString(value);}
    static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException exception){throw new IllegalStateException(exception);}}
}
