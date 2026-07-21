package com.gyeongsan.cabinet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.data.redis.stream.StreamListener;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@Log4j2
public class RedisStreamConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisTemplate<String, Object> redisTemplate;

    public static final String SLACK_STREAM_KEY = "slack-alarm-stream";
    public static final String LOGTIME_STREAM_KEY = "logtime-sync-stream";
    public static final String CONSUMER_GROUP_NAME = "cabinet-backend-group";
    private final String consumerName = UUID.randomUUID().toString();

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            StreamListener<String, MapRecord<String, String, String>> slackAlarmStreamListener,
            StreamListener<String, MapRecord<String, String, String>> logtimeStreamListener
    ) {
        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(redisConnectionFactory, options);

        createStreamGroup(SLACK_STREAM_KEY);
        createStreamGroup(LOGTIME_STREAM_KEY);

        container.receive(
                Consumer.from(CONSUMER_GROUP_NAME, consumerName),
                StreamOffset.create(SLACK_STREAM_KEY, ReadOffset.lastConsumed()),
                slackAlarmStreamListener
        );

        container.receive(
                Consumer.from(CONSUMER_GROUP_NAME, consumerName),
                StreamOffset.create(LOGTIME_STREAM_KEY, ReadOffset.lastConsumed()),
                logtimeStreamListener
        );

        container.start();
        return container;
    }

    private void createStreamGroup(String streamKey) {
        try {
            if (Boolean.FALSE.equals(redisTemplate.hasKey(streamKey))) {
                redisTemplate.opsForStream().add(streamKey, Collections.singletonMap("init", "init"));
                redisTemplate.opsForStream().createGroup(streamKey, CONSUMER_GROUP_NAME);
            } else {
                boolean groupExists = redisTemplate.opsForStream().groups(streamKey).stream()
                        .anyMatch(group -> group.groupName().equals(CONSUMER_GROUP_NAME));
                if (!groupExists) {
                    redisTemplate.opsForStream().createGroup(streamKey, CONSUMER_GROUP_NAME);
                }
            }
        } catch (Exception e) {
            log.warn("Redis Stream 그룹 초기화 예외 (무시 가능): {}", e.getMessage());
        }
    }
}
