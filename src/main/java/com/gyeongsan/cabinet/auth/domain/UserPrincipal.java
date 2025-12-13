package com.gyeongsan.cabinet.auth.domain;

import com.gyeongsan.cabinet.user.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class UserPrincipal implements OAuth2User {

    private final Long userId;
    private final String name;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    public UserPrincipal(User user, Map<String, Object> attributes) {
        this.userId = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.attributes = attributes;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("USER"));
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return name;
    }
}
