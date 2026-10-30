package com.gyeongsan.cabinet.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.client.connect-timeout}")
    private int connectTimeout;

    @Value("${app.client.read-timeout}")
    private int readTimeout;

    @Bean
    public WebClient webClient() {
        ConnectionProvider provider =
                ConnectionProvider.builder("web-client")
                        .maxIdleTime(Duration.ofSeconds(30))
                        .evictInBackground(Duration.ofSeconds(60))
                        .build();

        HttpClient httpClient =
                HttpClient.create(provider)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                        .responseTimeout(Duration.ofMillis(readTimeout));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
