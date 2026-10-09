package com.gyeongsan.cabinet.adapter.out.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.domain.kakaonotify.model.TokenDecryptionException;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AesGcmTokenCipherTest {

    private static String newKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    private final AesGcmTokenCipher cipher = new AesGcmTokenCipher(newKey());

    @Test
    @DisplayName("암호화한 값은 같은 컨텍스트로 복호화하면 원래 값이 되고, 암호문에는 평문이 보이지 않는다")
    void roundTrip() {
        String token = "refresh-token-abc123-한글도";

        String encrypted = cipher.encrypt(token, "kakao-notify:7");

        assertThat(encrypted).startsWith("v1.").doesNotContain("refresh-token");
        assertThat(cipher.decrypt(encrypted, "kakao-notify:7")).isEqualTo(token);
    }

    @Test
    @DisplayName("같은 값도 매번 다른 암호문이 나온다(IV 가 매번 새로 만들어진다)")
    void randomIv() {
        assertThat(cipher.encrypt("same", "ctx")).isNotEqualTo(cipher.encrypt("same", "ctx"));
    }

    @Test
    @DisplayName("다른 유저(컨텍스트)의 행에 옮겨 붙인 암호문은 복호화되지 않는다")
    void bindsToContext() {
        String encrypted = cipher.encrypt("token", "kakao-notify:1");

        assertThatThrownBy(() -> cipher.decrypt(encrypted, "kakao-notify:2"))
                .isInstanceOf(TokenDecryptionException.class);
    }

    @Test
    @DisplayName("암호문이 한 글자라도 바뀌었거나 형식이 틀리면 복호화에 실패한다")
    void detectsTampering() {
        String encrypted = cipher.encrypt("token", "ctx");
        char last = encrypted.charAt(encrypted.length() - 1);
        String tampered =
                encrypted.substring(0, encrypted.length() - 1) + (last == 'A' ? 'B' : 'A');

        assertThatThrownBy(() -> cipher.decrypt(tampered, "ctx"))
                .isInstanceOf(TokenDecryptionException.class);
        assertThatThrownBy(() -> cipher.decrypt("plain-text", "ctx"))
                .isInstanceOf(TokenDecryptionException.class);
        assertThatThrownBy(() -> cipher.decrypt("v1.", "ctx"))
                .isInstanceOf(TokenDecryptionException.class);
        assertThatThrownBy(() -> cipher.decrypt(null, "ctx"))
                .isInstanceOf(TokenDecryptionException.class);
    }

    @Test
    @DisplayName("키를 잃어 다른 키가 되면 기존 암호문은 전부 복호화할 수 없다(운영 주의 사항)")
    void lostKeyInvalidatesEverything() {
        String encrypted = cipher.encrypt("token", "ctx");

        AesGcmTokenCipher otherKey = new AesGcmTokenCipher(newKey());

        assertThatThrownBy(() -> otherKey.decrypt(encrypted, "ctx"))
                .isInstanceOf(TokenDecryptionException.class);
    }

    @Test
    @DisplayName("키는 base64 로 인코딩한 32바이트여야 하고, 잘못된 값은 키 내용을 노출하지 않고 거부한다")
    void rejectsBadKeys() {
        String secretLooking = "not-base64-@@@-super-secret";
        assertThatThrownBy(() -> new AesGcmTokenCipher(secretLooking))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageNotContaining("super-secret");
        assertThatThrownBy(
                        () ->
                                new AesGcmTokenCipher(
                                        Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AesGcmTokenCipher(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AesGcmTokenCipher(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
