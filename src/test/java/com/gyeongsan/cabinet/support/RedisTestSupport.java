package com.gyeongsan.cabinet.support;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/** 실제 Redis(비클러스터) 컨테이너와 그에 붙는 StringRedisTemplate 을 만든다. 운영/테스트 환경과 같은 비클러스터 구성이다. */
public final class RedisTestSupport {

    private RedisTestSupport() {}

    public static GenericContainer<?> newRedis() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
    }

    public static LettuceConnectionFactory connectionFactory(GenericContainer<?> redis) {
        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(
                        new RedisStandaloneConfiguration(
                                redis.getHost(), redis.getMappedPort(6379)));
        factory.afterPropertiesSet();
        return factory;
    }

    public static StringRedisTemplate template(LettuceConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        return template;
    }
}
