package com.gyeongsan.cabinet.adapter.out.crypto;

import com.gyeongsan.cabinet.domain.kakaonotify.model.TokenDecryptionException;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.TokenCipherPort;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM. 저장 형식은 {@code v1.<base64url(IV 12바이트 || 암호문 || 태그 16바이트)>} 이다.
 *
 * <ul>
 *   <li>값마다 새 무작위 IV 를 쓴다(같은 토큰도 매번 다른 암호문).
 *   <li>{@code context}(예: 유저 ID)를 인증 데이터(AAD)로 묶어, 다른 유저 행에 옮겨 붙인 값이나 변조된 값은 복호화에 실패한다.
 *   <li>맨 앞의 {@code v1.} 은 나중에 키를 바꿀 때 형식을 구분하기 위한 표시다.
 * </ul>
 *
 * 키(32바이트)를 잃어버리면 저장된 모든 토큰을 복호화할 수 없다.
 */
public class AesGcmTokenCipher implements TokenCipherPort {

    private static final String PREFIX = "v1.";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    /**
     * @param base64Key base64 로 인코딩한 32바이트 키. 형식이 틀리면 IllegalArgumentException(키 값은 메시지에 넣지 않는다)
     */
    public AesGcmTokenCipher(String base64Key) {
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key == null ? "" : base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("암호화 키는 base64 로 인코딩한 32바이트 값이어야 합니다.");
        }
        if (raw.length != 32) {
            Arrays.fill(raw, (byte) 0);
            throw new IllegalArgumentException("암호화 키는 base64 로 인코딩한 32바이트 값이어야 합니다.");
        }
        this.key = new SecretKeySpec(raw, "AES");
        Arrays.fill(raw, (byte) 0);
    }

    @Override
    public String encrypt(String plaintext, String context) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("토큰 암호화에 실패했습니다.", e);
        }
    }

    @Override
    public String decrypt(String ciphertext, String context) {
        try {
            if (ciphertext == null || !ciphertext.startsWith(PREFIX)) {
                throw new IllegalArgumentException("알 수 없는 암호문 형식");
            }
            byte[] in = Base64.getUrlDecoder().decode(ciphertext.substring(PREFIX.length()));
            if (in.length <= IV_BYTES) {
                throw new IllegalArgumentException("암호문이 너무 짧음");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_BITS, Arrays.copyOfRange(in, 0, IV_BYTES)));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] plain = cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // 원인 예외 메시지에는 값이 들어 있지 않지만, 그래도 암호문 자체는 로그에 남기지 않는다.
            throw new TokenDecryptionException("토큰을 복호화할 수 없습니다(키가 바뀌었거나 값이 손상됨).", e);
        }
    }
}
