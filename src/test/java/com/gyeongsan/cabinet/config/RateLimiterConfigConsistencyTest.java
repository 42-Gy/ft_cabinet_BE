package com.gyeongsan.cabinet.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

/** yml 에 정의한 RateLimiter 와 코드의 @RateLimiter 이름이 서로 어긋나지 않는지 확인한다(설정만 있고 안 쓰이는 한도 방지). */
class RateLimiterConfigConsistencyTest {

    private static final Path MAIN_YML = Path.of("src/main/resources/application.yml");
    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final Pattern YML_KEY =
            Pattern.compile("^resilience4j\\.ratelimiter\\.instances\\.([^.]+)\\..+$");
    private static final Pattern ANNOTATION =
            Pattern.compile("@RateLimiter\\(\\s*name\\s*=\\s*\"([^\"]+)\"");

    private static Set<String> configuredInYml() throws IOException {
        Set<String> names = new TreeSet<>();
        for (PropertySource<?> source :
                new YamlPropertySourceLoader()
                        .load("main-application-yml", new FileSystemResource(MAIN_YML))) {
            for (String key : ((EnumerablePropertySource<?>) source).getPropertyNames()) {
                Matcher m = YML_KEY.matcher(key);
                if (m.matches()) {
                    names.add(m.group(1));
                }
            }
        }
        return names;
    }

    private static Set<String> usedInCode() throws IOException {
        Set<String> names = new TreeSet<>();
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file :
                    (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                Matcher m = ANNOTATION.matcher(Files.readString(file));
                while (m.find()) {
                    names.add(m.group(1));
                }
            }
        }
        return names;
    }

    @Test
    @DisplayName("yml 에 정의된 RateLimiter 는 모두 코드에서 쓰이고, 코드가 쓰는 이름은 모두 yml 에 정의돼 있다")
    void ymlAndCodeAgree() throws IOException {
        Set<String> configured = configuredInYml();
        Set<String> used = usedInCode();

        assertThat(configured).as("yml 에는 있는데 코드에서 안 쓰이는 한도").isSubsetOf(used);
        assertThat(used).as("코드가 쓰는데 yml 에 없는 한도(기본값으로 동작함)").isSubsetOf(configured);
        assertThat(used).contains("userApi", "lentApi", "ftApi");
    }
}
