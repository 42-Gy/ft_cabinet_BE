package com.gyeongsan.cabinet.domain.auth.port.in;

public interface LinkAccountUseCase {
    void linkAccount(Long userId, String provider, String authorizationCode);
}
