package com.skala.shopping.auth.internal;

import java.time.Duration; import java.util.UUID;

interface RefreshSessionStore {
    String issue(UUID memberId,long credentialVersion,Duration ttl);
    RefreshPrincipal consume(String token);
    void revoke(String token);
    final class RefreshPrincipal { private final UUID memberId; private final long credentialVersion;
        RefreshPrincipal(UUID memberId,long credentialVersion){this.memberId=memberId;this.credentialVersion=credentialVersion;}
        UUID memberId(){return memberId;} long credentialVersion(){return credentialVersion;} }
}
