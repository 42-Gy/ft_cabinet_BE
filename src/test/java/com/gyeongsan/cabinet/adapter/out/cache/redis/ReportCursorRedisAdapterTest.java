package com.gyeongsan.cabinet.adapter.out.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.gyeongsan.cabinet.support.RedisTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class ReportCursorRedisAdapterTest {

    private static GenericContainer<?> redis;
    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate template;

    @BeforeAll
    static void start() {
        redis = RedisTestSupport.newRedis();
        redis.start();
        factory = RedisTestSupport.connectionFactory(redis);
        template = RedisTestSupport.template(factory);
    }

    @AfterAll
    static void stop() {
        if (factory != null) {
            factory.destroy();
        }
        if (redis != null) {
            redis.stop();
        }
    }

    @BeforeEach
    void flush() {
        template.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("커서가 없으면 비어 있고, 저장하면 읽히며, 채널마다 따로 저장된다")
    void roundTrip() {
        ReportCursorRedisAdapter adapter = new ReportCursorRedisAdapter(template);

        assertThat(adapter.getCursor("C1")).isEmpty();

        adapter.saveCursor("C1", "1700000100.000100");
        adapter.saveCursor("C2", "1700000200.000200");

        assertThat(adapter.getCursor("C1")).contains("1700000100.000100");
        assertThat(adapter.getCursor("C2")).contains("1700000200.000200");

        adapter.saveCursor("C1", "1700000300.000300");
        assertThat(adapter.getCursor("C1")).contains("1700000300.000300");
    }

    @Test
    @DisplayName("만료 시간을 두지 않는다")
    void noExpiry() {
        new ReportCursorRedisAdapter(template).saveCursor("C1", "1.0");

        assertThat(template.getExpire("slack:report:cursor:C1")).isEqualTo(-1L);
    }
}
