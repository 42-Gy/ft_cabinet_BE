package com.gyeongsan.cabinet.adapter.out.persistence.kakaonotify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gyeongsan.cabinet.adapter.out.crypto.AesGcmTokenCipher;
import com.gyeongsan.cabinet.adapter.out.persistence.user.UserRepository;
import com.gyeongsan.cabinet.application.kakaonotify.SlackNoticeForwardService;
import com.gyeongsan.cabinet.application.kakaonotify.SlackNoticeSettings;
import com.gyeongsan.cabinet.domain.alarm.model.SlackChannelMessage;
import com.gyeongsan.cabinet.domain.alarm.model.SlackHistory;
import com.gyeongsan.cabinet.domain.alarm.port.out.SlackChannelPort;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoGrant;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoInvalidGrantException;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoMessage;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoNotifyStatus;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoRecipient;
import com.gyeongsan.cabinet.domain.kakaonotify.model.KakaoRefreshedToken;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoConsentRepositoryPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.KakaoNotificationPort;
import com.gyeongsan.cabinet.domain.kakaonotify.port.out.NoticeCursorPort;
import com.gyeongsan.cabinet.support.MariaDbDriverMySqlContainer;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 MySQL 8.0/8.4 에서 동의 저장소의 동시성·해지·토큰 회전 시나리오를 확인한다. 스키마는 운영과 같은 Flyway(V1~V6)로 만들고, 엔티티는
 * ddl-auto=validate 로 맞춘다. Docker 가 없으면 건너뛴다.
 */
@Testcontainers(disabledWithoutDocker = true)
class KakaoConsentMysqlTest {

    @Configuration
    @EntityScan("com.gyeongsan.cabinet")
    @EnableJpaRepositories(
            basePackageClasses = {UserRepository.class, KakaoNotifyConsentRepository.class})
    @Import({KakaoConsentPersistenceAdapter.class, KakaoLoginLinkPersistenceAdapter.class})
    static class TestConfig {}

    private static final AtomicInteger SEQ = new AtomicInteger();

    private static String key() {
        byte[] k = new byte[32];
        new SecureRandom().nextBytes(k);
        return Base64.getEncoder().encodeToString(k);
    }

    private static long newUser(Connection c, String prefix) throws SQLException {
        String name = prefix + "-" + SEQ.incrementAndGet();
        try (Statement s = c.createStatement()) {
            s.execute(
                    "INSERT INTO `user` (name, email, role, is_pisciner) VALUES ('"
                            + name
                            + "', '"
                            + name
                            + "@42gyeongsan.kr', 'USER', b'0')");
            try (ResultSet rs =
                    s.executeQuery("SELECT id FROM `user` WHERE name = '" + name + "'")) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static void exec(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    private static boolean alarm(Connection c, long userId) throws SQLException {
        try (Statement s = c.createStatement();
                ResultSet rs =
                        s.executeQuery("SELECT kakao_alarm FROM `user` WHERE id = " + userId)) {
            rs.next();
            return rs.getBoolean(1);
        }
    }

    private static int consentRows(Connection c, long userId) throws SQLException {
        try (Statement s = c.createStatement();
                ResultSet rs =
                        s.executeQuery(
                                "SELECT COUNT(*) FROM kakao_notify_consent WHERE user_id = "
                                        + userId)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static <T> List<T> runConcurrently(int n, Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            CountDownLatch ready = new CountDownLatch(n);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                futures.add(
                        pool.submit(
                                () -> {
                                    ready.countDown();
                                    go.await();
                                    return task.call();
                                }));
            }
            ready.await();
            go.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> f : futures) {
                results.add(f.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"mysql:8.0", "mysql:8.4"})
    @DisplayName("동의 저장소: 첫 동의/재동의/해지/회전/동시 요청/수신 대상 조건이 실제 MySQL 에서 의도대로 동작한다")
    void consentLifecycle(String image) throws Exception {
        try (MariaDbDriverMySqlContainer mysql = new MariaDbDriverMySqlContainer(image)) {
            mysql.start();
            Flyway.configure()
                    .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            try (Connection c =
                    DriverManager.getConnection(
                            mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
                new ApplicationContextRunner()
                        .withConfiguration(
                                AutoConfigurations.of(
                                        DataSourceAutoConfiguration.class,
                                        TransactionAutoConfiguration.class,
                                        HibernateJpaAutoConfiguration.class,
                                        JpaRepositoriesAutoConfiguration.class))
                        .withUserConfiguration(TestConfig.class)
                        .withPropertyValues(
                                "spring.datasource.url=" + mysql.getJdbcUrl(),
                                "spring.datasource.username=" + mysql.getUsername(),
                                "spring.datasource.password=" + mysql.getPassword(),
                                "spring.jpa.hibernate.ddl-auto=validate",
                                "spring.jpa.open-in-view=false")
                        .run(
                                context -> {
                                    assertThat(context).hasNotFailed();
                                    KakaoConsentRepositoryPort repo =
                                            context.getBean(KakaoConsentRepositoryPort.class);

                                    firstGrantTurnsAlarmOn(c, repo);
                                    reConsentKeepsUsersSwitch(c, repo);
                                    revokedConsentIsReactivated(c, repo);
                                    recipientConditions(c, repo);
                                    revokeAndRotateAreCompareAndSet(c, repo);
                                    concurrentGrantsLeaveOneRow(c, repo);
                                    concurrentRotationHasOneWinner(c, repo);
                                    forwarderAgainstRealDatabase(c, repo);
                                    loginLinkLookup(
                                            c,
                                            context.getBean(
                                                    KakaoLoginLinkPersistenceAdapter.class));
                                });
            }
        }
    }

    private void firstGrantTurnsAlarmOn(Connection c, KakaoConsentRepositoryPort repo)
            throws Exception {
        long user = newUser(c, "first");
        assertThat(repo.findStatus(user)).isEqualTo(new KakaoNotifyStatus(false, false));

        boolean activated = repo.grant(user, "enc-1", "talk_message", LocalDateTime.now());

        assertThat(activated).as("첫 동의는 알림 스위치를 켠다").isTrue();
        assertThat(repo.findStatus(user)).isEqualTo(new KakaoNotifyStatus(true, true));
        assertThat(alarm(c, user)).isTrue();
        assertThat(repo.findActiveRecipient(user)).isPresent();
    }

    private void reConsentKeepsUsersSwitch(Connection c, KakaoConsentRepositoryPort repo)
            throws Exception {
        long user = newUser(c, "reconsent");
        repo.grant(user, "enc-1", "talk_message", LocalDateTime.now());
        exec(c, "UPDATE `user` SET kakao_alarm = b'0' WHERE id = " + user); // 유저가 스위치를 끔

        boolean activated = repo.grant(user, "enc-2", "talk_message", LocalDateTime.now());

        assertThat(activated).as("이미 유효한 동의를 다시 받아도 꺼 둔 스위치를 되돌리지 않는다").isFalse();
        assertThat(alarm(c, user)).isFalse();
        assertThat(consentRows(c, user)).isEqualTo(1);
        // 토큰은 새 값으로 바뀌었다(스위치가 꺼져 있어 수신 대상은 아님).
        assertThat(repo.findActiveRecipient(user)).isEmpty();
        exec(c, "UPDATE `user` SET kakao_alarm = b'1' WHERE id = " + user);
        assertThat(repo.findActiveRecipient(user).orElseThrow().encryptedRefreshToken())
                .isEqualTo("enc-2");
    }

    private void revokedConsentIsReactivated(Connection c, KakaoConsentRepositoryPort repo)
            throws Exception {
        long user = newUser(c, "revoked");
        repo.grant(user, "enc-old", "talk_message", LocalDateTime.now());
        assertThat(repo.revokeIfTokenUnchanged(user, "enc-old", LocalDateTime.now())).isTrue();
        assertThat(repo.findStatus(user).consented()).isFalse();
        assertThat(repo.findActiveRecipient(user)).as("해지되면 대상에서 빠진다").isEmpty();

        boolean activated = repo.grant(user, "enc-new", "talk_message", LocalDateTime.now());

        assertThat(activated).as("해지된 동의를 다시 받으면 되살리고 스위치도 켠다").isTrue();
        assertThat(repo.findStatus(user)).isEqualTo(new KakaoNotifyStatus(true, true));
        assertThat(consentRows(c, user)).isEqualTo(1);
    }

    private void recipientConditions(Connection c, KakaoConsentRepositoryPort repo)
            throws Exception {
        long ok = newUser(c, "cond-ok");
        long off = newUser(c, "cond-off");
        long revoked = newUser(c, "cond-revoked");
        long deleted = newUser(c, "cond-deleted");
        long noConsent = newUser(c, "cond-none");
        for (long u : new long[] {ok, off, revoked, deleted}) {
            repo.grant(u, "enc-" + u, "talk_message", LocalDateTime.now());
        }
        exec(c, "UPDATE `user` SET kakao_alarm = b'0' WHERE id = " + off);
        repo.revokeIfTokenUnchanged(revoked, "enc-" + revoked, LocalDateTime.now());
        exec(c, "UPDATE `user` SET deleted_at = NOW(6) WHERE id = " + deleted);

        List<Long> ids = repo.findActiveRecipients().stream().map(KakaoRecipient::userId).toList();

        assertThat(ids).contains(ok).doesNotContain(off, revoked, deleted, noConsent);
        assertThat(repo.findActiveRecipient(off)).isEmpty();
        assertThat(repo.findActiveRecipient(deleted)).isEmpty();
    }

    private void revokeAndRotateAreCompareAndSet(Connection c, KakaoConsentRepositoryPort repo)
            throws Exception {
        long user = newUser(c, "cas");
        repo.grant(user, "enc-A", "talk_message", LocalDateTime.now());

        // 재동의로 토큰이 enc-B 로 바뀐 뒤에 옛 토큰(enc-A)으로 해지·회전을 시도하면 무시된다.
        repo.grant(user, "enc-B", "talk_message", LocalDateTime.now());
        assertThat(repo.revokeIfTokenUnchanged(user, "enc-A", LocalDateTime.now())).isFalse();
        assertThat(repo.rotateIfTokenUnchanged(user, "enc-A", "enc-X")).isFalse();
        assertThat(repo.findStatus(user).consented()).isTrue();
        assertThat(repo.findActiveRecipient(user).orElseThrow().encryptedRefreshToken())
                .isEqualTo("enc-B");

        // 읽은 값 그대로면 회전된다.
        assertThat(repo.rotateIfTokenUnchanged(user, "enc-B", "enc-C")).isTrue();
        assertThat(repo.findActiveRecipient(user).orElseThrow().encryptedRefreshToken())
                .isEqualTo("enc-C");

        // 해지된 동의는 회전되지 않고, 이미 해지된 것을 다시 해지해도 false.
        assertThat(repo.revokeIfTokenUnchanged(user, "enc-C", LocalDateTime.now())).isTrue();
        assertThat(repo.rotateIfTokenUnchanged(user, "enc-C", "enc-D")).isFalse();
        assertThat(repo.revokeIfTokenUnchanged(user, "enc-C", LocalDateTime.now())).isFalse();
    }

    private void concurrentGrantsLeaveOneRow(Connection c, KakaoConsentRepositoryPort repo)
            throws Exception {
        long user = newUser(c, "race-grant");

        List<Boolean> results =
                runConcurrently(
                        8,
                        () ->
                                repo.grant(
                                        user,
                                        "enc-" + Thread.currentThread().getId(),
                                        "talk_message",
                                        LocalDateTime.now()));

        assertThat(consentRows(c, user)).as("동시에 동의해도 한 줄만 남는다(UNIQUE 위반으로 실패하지 않는다)").isEqualTo(1);
        assertThat(results).as("'첫 동의'로 판정되는 것은 정확히 한 번").filteredOn(b -> b).hasSize(1);
        assertThat(repo.findStatus(user)).isEqualTo(new KakaoNotifyStatus(true, true));

        // DB 제약도 직접 확인: 같은 유저로 두 번째 행은 들어갈 수 없다.
        assertThatThrownBy(
                        () ->
                                exec(
                                        c,
                                        "INSERT INTO kakao_notify_consent (user_id, encrypted_refresh_token, scope, consented_at)"
                                                + " VALUES ("
                                                + user
                                                + ", 'x', 'talk_message', NOW(6))"))
                .isInstanceOf(SQLException.class);
    }

    private void concurrentRotationHasOneWinner(Connection c, KakaoConsentRepositoryPort repo)
            throws Exception {
        long user = newUser(c, "race-rotate");
        repo.grant(user, "enc-0", "talk_message", LocalDateTime.now());

        List<Boolean> results =
                runConcurrently(
                        8,
                        () ->
                                repo.rotateIfTokenUnchanged(
                                        user, "enc-0", "enc-" + Thread.currentThread().getId()));

        assertThat(results).as("같은 토큰에서 시작한 동시 회전은 한 번만 반영된다").filteredOn(b -> b).hasSize(1);
        assertThat(repo.findActiveRecipient(user).orElseThrow().encryptedRefreshToken())
                .isNotEqualTo("enc-0");
    }

    private void loginLinkLookup(Connection c, KakaoLoginLinkPersistenceAdapter adapter)
            throws Exception {
        long linked = newUser(c, "link-kakao");
        long googleOnly = newUser(c, "link-google");
        long none = newUser(c, "link-none");
        exec(
                c,
                "INSERT INTO oauth_link (user_id, provider, provider_id, linked_at) VALUES ("
                        + linked
                        + ", 'kakao', '999111', NOW(6))");
        exec(
                c,
                "INSERT INTO oauth_link (user_id, provider, provider_id, linked_at) VALUES ("
                        + googleOnly
                        + ", 'google', 'g-1', NOW(6))");

        assertThat(adapter.findKakaoProviderId(linked)).contains("999111");
        assertThat(adapter.findKakaoProviderId(googleOnly)).as("구글 연동은 카카오 연동이 아니다").isEmpty();
        assertThat(adapter.findKakaoProviderId(none)).isEmpty();
    }

    /** 가짜 카카오 + 실제 DB·암호화로 전달기를 끝까지 돌려, 해지·회전이 DB 에 실제로 반영되는지 본다. */
    private void forwarderAgainstRealDatabase(Connection c, KakaoConsentRepositoryPort repo)
            throws Exception {
        AesGcmTokenCipher cipher = new AesGcmTokenCipher(key());
        long fine = newUser(c, "fwd-fine");
        long gone = newUser(c, "fwd-gone");
        repo.grant(
                fine,
                cipher.encrypt("rt-fine", "kakao-notify:" + fine),
                "talk_message",
                LocalDateTime.now());
        repo.grant(
                gone,
                cipher.encrypt("rt-gone", "kakao-notify:" + gone),
                "talk_message",
                LocalDateTime.now());

        Map<String, String> sentTo = new HashMap<>();
        KakaoNotificationPort kakao =
                new KakaoNotificationPort() {
                    @Override
                    public KakaoGrant exchangeAuthorizationCode(String code, String redirectUri) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public KakaoRefreshedToken refreshAccessToken(String refreshToken) {
                        if (refreshToken.equals("rt-gone")) {
                            throw new KakaoInvalidGrantException(
                                    "invalid_grant"); // 유저가 카카오에서 동의 해지
                        }
                        return new KakaoRefreshedToken("at-" + refreshToken, "rt-fine-rotated");
                    }

                    @Override
                    public void sendToMe(String accessToken, KakaoMessage message) {
                        sentTo.put(accessToken, message.text());
                    }
                };
        Map<String, String> cursorStore = new HashMap<>();
        NoticeCursorPort cursor =
                new NoticeCursorPort() {
                    @Override
                    public Optional<String> getCursor(String channelId) {
                        return Optional.ofNullable(cursorStore.get(channelId));
                    }

                    @Override
                    public void saveCursor(String channelId, String ts) {
                        cursorStore.put(channelId, ts);
                    }
                };
        SlackChannelPort channel =
                new SlackChannelPort() {
                    @Override
                    public SlackHistory fetchNewerThan(String channelId, String oldestTs) {
                        return new SlackHistory(
                                List.of(
                                        new SlackChannelMessage(
                                                "1700000100.000100", "U1", "DB 공지", null, 0, 0)),
                                false);
                    }

                    @Override
                    public Optional<String> latestTs(String channelId) {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<String> permalink(String channelId, String ts) {
                        return Optional.empty();
                    }
                };
        cursorStore.put("C0DB", "1700000000.000000");
        SlackNoticeForwardService service =
                new SlackNoticeForwardService(
                        channel,
                        cursor,
                        repo,
                        kakao,
                        cipher,
                        new SlackNoticeSettings("C0DB", 5, 24, "https://front.example"),
                        Clock.fixed(Instant.ofEpochSecond(1_700_000_500L), ZoneOffset.UTC));

        service.forwardNewNotices();

        assertThat(sentTo).containsOnlyKeys("at-rt-fine");
        assertThat(repo.findStatus(gone).consented()).as("invalid_grant 면 DB 에서 해지 처리된다").isFalse();
        assertThat(repo.findStatus(fine).consented()).isTrue();
        // 회전된 refresh_token 이 암호화된 채 저장되어 있고 복호화하면 새 값이다.
        String stored = repo.findActiveRecipient(fine).orElseThrow().encryptedRefreshToken();
        assertThat(stored).doesNotContain("rt-fine-rotated");
        assertThat(cipher.decrypt(stored, "kakao-notify:" + fine)).isEqualTo("rt-fine-rotated");
    }
}
