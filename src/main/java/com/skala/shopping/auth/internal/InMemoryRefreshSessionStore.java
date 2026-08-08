package com.skala.shopping.auth.internal;

import com.skala.shopping.common.BusinessException; import com.skala.shopping.common.ErrorCode;
import java.time.*; import java.util.UUID; import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; import org.springframework.stereotype.Component;

@Component @ConditionalOnProperty(name="shopping.security.refresh-token.store",havingValue="memory",matchIfMissing=true)
class InMemoryRefreshSessionStore implements RefreshSessionStore {
    private final ConcurrentHashMap<String,Session> sessions=new ConcurrentHashMap<>(); private final Clock clock=Clock.systemUTC();
    public String issue(UUID id,long version,Duration ttl){String token=RefreshTokenSupport.generate();sessions.put(RefreshTokenSupport.hash(token),new Session(id,version,clock.instant().plus(ttl)));return token;}
    public RefreshPrincipal consume(String token){Session session=token==null?null:sessions.remove(RefreshTokenSupport.hash(token));
        if(session==null||!clock.instant().isBefore(session.expiresAt))throw invalid();return new RefreshPrincipal(session.id,session.version);}
    public void revoke(String token){if(token!=null)sessions.remove(RefreshTokenSupport.hash(token));}
    private BusinessException invalid(){return new BusinessException(ErrorCode.NOT_AUTHENTICATED,"리프레시 토큰이 유효하지 않습니다.");}
    private static final class Session {
        private final UUID id; private final long version; private final Instant expiresAt;
        private Session(UUID id,long version,Instant expiresAt){this.id=id;this.version=version;this.expiresAt=expiresAt;}
    }
}
