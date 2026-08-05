package com.skala.shopping.auth.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("shopping.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Cookie cookie = new Cookie();
    private Cors cors = new Cors();
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();

    @Valid
    private RateLimit rateLimit = new RateLimit();

    public SecurityProperties() {
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Cookie getCookie() {
        return cookie;
    }

    public void setCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    public Cors getCors() {
        return cors;
    }

    public void setCors(Cors cors) {
        this.cors = cors;
    }

    public BootstrapAdmin getBootstrapAdmin() {
        return bootstrapAdmin;
    }

    public void setBootstrapAdmin(BootstrapAdmin bootstrapAdmin) {
        this.bootstrapAdmin = bootstrapAdmin;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public static class Jwt {

        private String issuer;
        private String secret;
        private Duration accessTokenTtl;

        public Jwt() {
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }
    }

    public static class Cookie {

        private String name;
        private boolean secure;
        private String sameSite;

        public Cookie() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public String getSameSite() {
            return sameSite;
        }

        public void setSameSite(String sameSite) {
            this.sameSite = sameSite;
        }
    }

    public static class Cors {

        private List<String> allowedOrigins;

        public Cors() {
        }

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class BootstrapAdmin {

        private boolean enabled;
        private String loginId;
        private String password;

        public BootstrapAdmin() {
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getLoginId() {
            return loginId;
        }

        public void setLoginId(String loginId) {
            this.loginId = loginId;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RateLimit {

        private boolean enabled = true;

        @Min(2)
        private int maxTrackedKeys = 50_000;

        @Valid
        @NotNull
        private EndpointLimit login = new EndpointLimit(100, 20, Duration.ofMinutes(1));

        @Valid
        @NotNull
        private EndpointLimit registration = new EndpointLimit(30, 5, Duration.ofMinutes(10));

        @Valid
        @NotNull
        private EndpointLimit passwordReset = new EndpointLimit(20, 5, Duration.ofMinutes(10));

        public RateLimit() {
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxTrackedKeys() {
            return maxTrackedKeys;
        }

        public void setMaxTrackedKeys(int maxTrackedKeys) {
            this.maxTrackedKeys = maxTrackedKeys;
        }

        public EndpointLimit getLogin() {
            return login;
        }

        public void setLogin(EndpointLimit login) {
            this.login = login;
        }

        public EndpointLimit getRegistration() {
            return registration;
        }

        public void setRegistration(EndpointLimit registration) {
            this.registration = registration;
        }

        public EndpointLimit getPasswordReset() {
            return passwordReset;
        }

        public void setPasswordReset(EndpointLimit passwordReset) {
            this.passwordReset = passwordReset;
        }
    }

    public static class EndpointLimit {

        @Min(1)
        private int maxRequestsPerIp;

        @Min(1)
        private int maxRequestsPerAccount;

        @NotNull
        private Duration window;

        public EndpointLimit() {
        }

        EndpointLimit(int maxRequestsPerIp, int maxRequestsPerAccount, Duration window) {
            this.maxRequestsPerIp = maxRequestsPerIp;
            this.maxRequestsPerAccount = maxRequestsPerAccount;
            this.window = window;
        }

        public int getMaxRequestsPerIp() {
            return maxRequestsPerIp;
        }

        public void setMaxRequestsPerIp(int maxRequestsPerIp) {
            this.maxRequestsPerIp = maxRequestsPerIp;
        }

        public int getMaxRequestsPerAccount() {
            return maxRequestsPerAccount;
        }

        public void setMaxRequestsPerAccount(int maxRequestsPerAccount) {
            this.maxRequestsPerAccount = maxRequestsPerAccount;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        @AssertTrue(message = "rate limit window must be greater than zero")
        public boolean isWindowPositive() {
            return window != null && !window.isZero() && !window.isNegative();
        }
    }
}
