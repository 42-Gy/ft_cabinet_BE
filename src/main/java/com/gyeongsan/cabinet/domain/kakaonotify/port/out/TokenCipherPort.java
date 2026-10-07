package com.gyeongsan.cabinet.domain.kakaonotify.port.out;

import com.gyeongsan.cabinet.domain.kakaonotify.model.TokenDecryptionException;

/** 토큰 암복호화. {@code context} 는 암호문을 특정 용도·유저에 묶는다(다른 유저 행으로 옮겨 붙인 값은 복호화되지 않는다). */
public interface TokenCipherPort {

    String encrypt(String plaintext, String context);

    /**
     * @throws TokenDecryptionException 복호화할 수 없는 값
     */
    String decrypt(String ciphertext, String context);
}
