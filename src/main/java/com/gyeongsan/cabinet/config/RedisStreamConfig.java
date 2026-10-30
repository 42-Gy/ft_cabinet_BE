package com.gyeongsan.cabinet.config;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamReadRequest;

@Configuration
@RequiredArgsConstructor
@Log4j2
public class RedisStreamConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final StringRedisTemplate stringRedisTemplate;

    public static final String SLACK_STREAM_KEY = "slack-alarm-stream";
    public static final String LOGTIME_STREAM_KEY = "logtime-sync-stream";
    public static final String CONSUMER_GROUP_NAME = "cabinet-backend-group";
    static final Duration READ_ERROR_BACKOFF = Duration.ofSeconds(5);
    @Getter private final String consumerName = UUID.randomUUID().toString();

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>>
            streamMessageListenerContainer(
                    StreamListener<String, MapRecord<String, String, String>>
                            slackAlarmStreamListener,
                    StreamListener<String, MapRecord<String, String, String>>
                            logtimeStreamListener) {
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        @SuppressWarnings("unchecked")
        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .serializer(stringSerializer)
                        .errorHandler(RedisStreamConfig::backOffOnReadError)
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        createStreamGroup(SLACK_STREAM_KEY);
        createStreamGroup(LOGTIME_STREAM_KEY);

        subscribe(container, SLACK_STREAM_KEY, slackAlarmStreamListener);
        subscribe(container, LOGTIME_STREAM_KEY, logtimeStreamListener);

        container.start();
        return container;
    }

    static void backOffOnReadError(Throwable t) {
        log.warn(
                "Redis Stream 읽기 실패, {}초 후 재시도: {}",
                READ_ERROR_BACKOFF.toSeconds(),
                t.getMessage());
        try {
            Thread.sleep(READ_ERROR_BACKOFF.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void subscribe(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            String streamKey,
            StreamListener<String, MapRecord<String, String, String>> listener) {
        container.register(
                StreamReadRequest.builder(StreamOffset.create(streamKey, ReadOffset.lastConsumed()))
                        .consumer(Consumer.from(CONSUMER_GROUP_NAME, consumerName))
                        .autoAcknowledge(false)
                        .cancelOnError(e -> false)
                        .build(),
                listener);
    }

    private void createStreamGroup(String streamKey) {
        try {
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(streamKey))) {
                stringRedisTemplate
                        .opsForStream()
                        .add(streamKey, Collections.singletonMap("init", "init"));
                stringRedisTemplate.opsForStream().createGroup(streamKey, CONSUMER_GROUP_NAME);
            } else {
                boolean groupExists =
                        stringRedisTemplate.opsForStream().groups(streamKey).stream()
                                .anyMatch(group -> group.groupName().equals(CONSUMER_GROUP_NAME));
                if (!groupExists) {
                    stringRedisTemplate.opsForStream().createGroup(streamKey, CONSUMER_GROUP_NAME);
                }
            }
        } catch (Exception e) {
            log.warn("Redis Stream 그룹 초기화 예외 (무시 가능): {}", e.getMessage());
        }
    }
}
