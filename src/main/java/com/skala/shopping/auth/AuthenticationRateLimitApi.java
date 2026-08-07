package com.skala.shopping.auth;

/**
 * 공개 인증 요청에 적용하는 요청 제한 포트입니다.
 *
 * <p>현재 구현은 단일 애플리케이션 인스턴스의 메모리를 사용합니다. 여러 인스턴스로
 * 확장할 때에는 storefront 호출부를 변경하지 않고 공유 저장소 기반 구현으로 교체합니다.</p>
 */
public interface AuthenticationRateLimitApi {

    void checkLogin(String clientAddress, String loginId);

    void checkRegistration(String clientAddress, String loginId);

    void checkPasswordReset(String clientAddress, String loginId);
}
