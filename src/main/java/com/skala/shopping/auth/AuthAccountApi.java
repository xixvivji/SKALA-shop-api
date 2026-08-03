package com.skala.shopping.auth;

import java.util.UUID;

public interface AuthAccountApi {

    void createAccount(UUID memberId, String loginId, String rawPassword);

    void deactivateAccount(UUID memberId);
}
