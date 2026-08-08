package com.skala.shopping.auth.internal;

import com.skala.shopping.common.BusinessException; import com.skala.shopping.common.ErrorCode;
import java.time.Duration; import java.util.UUID; import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate; import org.springframework.stereotype.Component;

@Component @ConditionalOnProperty(name="shopping.security.refresh-token.store",havingValue="redis")
class RedisRefreshSessionStore implements RefreshSessionStore {
    private final StringRedisTemplate redis; RedisRefreshSessionStore(StringRedisTemplate redis){this.redis=redis;}
    public String issue(UUID id,long version,Duration ttl){String token=RefreshTokenSupport.generate();redis.opsForValue().set(key(token),id+":"+version,ttl);return token;}
    public RefreshPrincipal consume(String token){String value=token==null?null:redis.opsForValue().getAndDelete(key(token));
        if(value==null)throw invalid();try{int split=value.lastIndexOf(':');return new RefreshPrincipal(UUID.fromString(value.substring(0,split)),Long.parseLong(value.substring(split+1)));}
        catch(RuntimeException exception){throw invalid();}}
    public void revoke(String token){if(token!=null)redis.delete(key(token));}
    private String key(String token){return "skala:auth:refresh:"+RefreshTokenSupport.hash(token);}
    private BusinessException invalid(){return new BusinessException(ErrorCode.NOT_AUTHENTICATED,"리프레시 토큰이 유효하지 않습니다.");}
}
