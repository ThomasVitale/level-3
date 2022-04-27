package functions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Level3Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
public class Level3ApplicationTests {

    private static final int REDIS_PORT = 6379;

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:6.2"))
            .withExposedPorts(REDIS_PORT);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", () -> redis.getHost());
        registry.add("spring.redis.port", () -> redis.getMappedPort(REDIS_PORT));
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void whenAnswersSubmittedThenGameScoreReturned() {
        var answers = new Answers(UUID.randomUUID().toString(), 42);
        webTestClient
                .post()
                .uri("/uppercase")
                .bodyValue(answers)
                .exchange()
                .expectStatus().isOk()
                .expectBody(GameScore.class).value(gameScore -> {
                    assertThat(gameScore.sessionId()).isEqualTo(answers.sessionId());
                    assertThat(gameScore.gameTime()).isNotNull();
                    assertThat(gameScore.level()).isEqualTo("level-3");
                    assertThat(gameScore.levelScore()).isEqualTo(answers.counter());
                });
    }

}
