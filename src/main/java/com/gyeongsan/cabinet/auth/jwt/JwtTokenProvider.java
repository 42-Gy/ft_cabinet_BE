package com.gyeongsan.cabinet.auth.jwt;

import com.gyeongsan.cabinet.auth.domain.UserPrincipal;
import com.gyeongsan.cabinet.user.domain.User;
import com.gyeongsan.cabinet.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Collections;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Log4j2
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    private final UserRepository userRepository;

    private Key key;

    @Value("${jwt.access-token-validity}")
    private long tokenValidTime;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidTime;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(secretKey.getBytes()));
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createToken(Long userId, String name, String role) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        claims.put("name", name);
        claims.put("role", role);

        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + tokenValidTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        Date now = new Date();

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenValidTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("유효하지 않은 토큰: {}", e.getMessage());
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        Long userId = Long.valueOf(claims.getSubject());

        String name = claims.get("name", String.class);
        String roleStr = claims.get("role", String.class);

        User user = User.builder()
                .id(userId)
                .name(name)
                .role(com.gyeongsan.cabinet.user.domain.UserRole.valueOf(roleStr))
                .build();

        UserPrincipal userPrincipal = new UserPrincipal(user, Collections.emptyMap());

        return new UsernamePasswordAuthenticationToken(
                userPrincipal, "", userPrincipal.getAuthorities());
    }
}
