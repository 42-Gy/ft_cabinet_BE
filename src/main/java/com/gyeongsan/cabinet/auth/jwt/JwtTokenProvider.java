package com.gyeongsan.cabinet.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Component
@Log4j2
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    private Key key;
    private final long TOKEN_VALID_TIME = 30 * 60 * 1000L; // 30분 (Access Token)
    private final long REFRESH_TOKEN_VALID_TIME = 14 * 24 * 60 * 60 * 1000L; // 14일 (Refresh Token)

    // 1. 비밀키를 암호화 객체로 변환 (서버 켜질 때 한 번 실행)
    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(secretKey.getBytes()));
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 2. Access Token 생성 (입장권 발급 - 30분)
    public String createToken(Long userId, String name, String role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId)); // 토큰 제목(PK)
        claims.put("name", name); // 추가 정보
        claims.put("role", role);

        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now) // 발행 시간
                .setExpiration(new Date(now.getTime() + TOKEN_VALID_TIME)) // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 암호화 알고리즘
                .compact();
    }

    // 3. [Ver 3.5 추가] Refresh Token 생성 (재발급용 - 14일)
    // Refresh Token은 보안상 유저 정보(Claims)를 최소화합니다. (오직 userId만 포함)
    public String createRefreshToken(Long userId) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_VALID_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 4. 토큰에서 정보 꺼내기 (검표)
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 5. 토큰 유효성 검사 (Access/Refresh 공용)
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("🚨 유효하지 않은 토큰: {}", e.getMessage());
            return false;
        }
    }

    // 6. 토큰에서 인증 정보(Authentication) 조회 - 필터에서 사용
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        // 토큰에 담긴 권한 정보 가져오기
        String role = claims.get("role", String.class);
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

        // SecurityContext에 저장할 객체 생성 (Principal: userId, Credentials: 빈값, Authorities: 권한)
        return new UsernamePasswordAuthenticationToken(claims.getSubject(), "", authorities);
    }
}