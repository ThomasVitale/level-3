package com.thomasvitale.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thomasvitale.game.config.GameEventingProperties;
import com.thomasvitale.game.model.Answers;
import com.thomasvitale.game.model.GameScore;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

@Configuration
public class Functions {

    private static final Logger log = LoggerFactory.getLogger(Functions.class);
    private static final String LEVEL_NAME = "level-3";
    private final GameEventingProperties gameEventingProperties;
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final WebClient webClient;

    public Functions(GameEventingProperties gameEventingProperties, ObjectMapper objectMapper, ReactiveStringRedisTemplate redisTemplate, WebClient.Builder webClientBuilder) {
        this.gameEventingProperties = gameEventingProperties;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.webClient = webClientBuilder
                .baseUrl(gameEventingProperties.brokerUri().toString())
                .build();
    }

    @Bean
    public Function<Mono<Answers>, Mono<GameScore>> answers() {
        return answers -> answers
                .map(this::scoreLevel)
                .flatMap(this::processLevel);
    }

    private GameScore scoreLevel(Answers answers) {
        log.debug("Answers counter in Level 3: {}", answers.counter());
        int points = answers.counter() > 1 ? answers.counter() : -1;
        return new GameScore(answers.player(), answers.sessionId(), LocalDateTime.now(), LEVEL_NAME, points);
    }

    private Mono<GameScore> processLevel(GameScore gameScore) {
        return redisTemplate.opsForList().rightPush("score-" + gameScore.sessionId(), writeValueAsString(gameScore))
                .then(publishEvent(gameScore));
    }

    private Mono<GameScore> publishEvent(GameScore gameScore) {
        if (!gameEventingProperties.enabled()) {
            return Mono.just(gameScore);
        }

        var cloudEvent = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType("GameScoreEvent")
                .withSource(URI.create("level-3.default.svc.cluster.local"))
                .withData(writeValueAsString(gameScore).getBytes(StandardCharsets.UTF_8))
                .withDataContentType(MediaType.APPLICATION_JSON_VALUE)
                .withSubject(gameScore.sessionId())
                .build();

        return webClient
                .post()
                .bodyValue(cloudEvent)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(result -> log.info("Published event. Id: {}, type: {}, session: {}.", cloudEvent.getId(), cloudEvent.getType(), gameScore.sessionId()))
                .doOnError(result -> log.error("Error publishing event. Cause: {}. Message: {}", result.getCause(), result.getMessage()))
                .thenReturn(gameScore);
    }

    private String writeValueAsString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Error when serializing Score", ex);
        }
    }

}
