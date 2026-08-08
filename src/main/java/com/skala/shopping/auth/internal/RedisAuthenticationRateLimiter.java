package com.skala.shopping.auth.internal;

import com.skala.shopping.auth.AuthenticationRateLimitApi;
import com.skala.shopping.common.RateLimitExceededException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 여러 애플리케이션 인스턴스가 동일한 인증 요청 횟수를 공유하는 Redis 고정 윈도우 제한기입니다. */
@Component
@ConditionalOnProperty(name = "shopping.security.rate-limit.store", havingValue = "redis")
class RedisAuthenticationRateLimiter implements AuthenticationRateLimitApi {
    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>(
            "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('PEXPIRE',KEYS[1],ARGV[1]) end; return n",
            Long.class);
    private final StringRedisTemplate redis; private final SecurityProperties properties;
    RedisAuthenticationRateLimiter(StringRedisTemplate redis, SecurityProperties properties) {
        this.redis=redis; this.properties=properties;
    }
    public void checkLogin(String ip,String id){check("login",ip,id,properties.getRateLimit().getLogin());}
    public void checkRegistration(String ip,String id){check("registration",ip,id,properties.getRateLimit().getRegistration());}
    public void checkPasswordReset(String ip,String id){check("password-reset",ip,id,properties.getRateLimit().getPasswordReset());}
    private void check(String operation,String ip,String account,SecurityProperties.EndpointLimit limit){
        if(!properties.getRateLimit().isEnabled())return;
        consume(operation+":ip:"+hash(normalize(ip,false)),limit.getMaxRequestsPerIp(),limit.getWindow());
        consume(operation+":account:"+hash(normalize(account,true)),limit.getMaxRequestsPerAccount(),limit.getWindow());
    }
    private void consume(String suffix,int maximum,Duration window){
        String key="skala:auth:rate:"+suffix;
        Long count=redis.execute(INCREMENT,List.of(key),String.valueOf(window.toMillis()));
        if(count!=null&&count>maximum){Long ttl=redis.getExpire(key);throw new RateLimitExceededException(Math.max(1,ttl==null?1:ttl));}
    }
    private String normalize(String value,boolean lower){String normalized=StringUtils.hasText(value)?value.trim():"unknown";
        return lower?normalized.toLowerCase(Locale.ROOT):normalized;}
    private String hash(String value){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(digest);}catch(NoSuchAlgorithmException exception){throw new IllegalStateException(exception);}}
}
