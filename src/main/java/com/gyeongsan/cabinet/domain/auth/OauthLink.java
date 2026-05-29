package com.gyeongsan.cabinet.domain.auth;

import com.gyeongsan.cabinet.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "OAUTH_LINK", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"PROVIDER", "PROVIDER_ID"})
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OauthLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Column(name = "PROVIDER", nullable = false, length = 20)
    private String provider;

    @Column(name = "PROVIDER_ID", nullable = false, length = 100)
    private String providerId;

    @Column(name = "PROVIDER_EMAIL", length = 100)
    private String providerEmail;

    @Column(name = "LINKED_AT", nullable = false)
    private LocalDateTime linkedAt;
}
