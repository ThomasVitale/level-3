package com.thomasvitale.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thomasvitale.game.config.GameEventingProperties;
import com.thomasvitale.game.model.Answers;
import com.thomasvitale.game.model.GameScore;
import com.thomasvitale.game.model.GameTime;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.net.URI;
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

    private Tuple2<GameScore, GameTime> scoreLevel(Answers answers) {
        log.debug("Answers counter in Level 3: {}", answers.counter());
        int points = answers.counter() > 1 ? answers.counter() : -1;
        var gameScore = new GameScore(answers.sessionId(), LocalDateTime.now(), LEVEL_NAME, points);
        var gameTime = new GameTime("time-" + gameScore.sessionId(), gameScore.sessionId(), gameScore.level(), "end", gameScore.gameTime());
        return Tuples.of(gameScore, gameTime);
    }

    private Mono<GameScore> processLevel(Tuple2<GameScore,GameTime> tuple) {
        return redisTemplate.opsForList().rightPush("score-" + tuple.getT1().sessionId(), writeValueAsString(tuple.getT1()))
                .then(redisTemplate.opsForList().rightPush(tuple.getT2().gameTimeId(), writeValueAsString(tuple.getT2())))
                .then(gameEventingProperties.enabled() ? webClient.post().uri("/").bodyValue(buildCloudEvent(tuple.getT1())).retrieve().toBodilessEntity().log() : Mono.empty())
                .then(Mono.just(tuple.getT1()));
    }

    private String writeValueAsString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Error when serializing Score", ex);
        }
    }

    private CloudEvent buildCloudEvent(GameScore gameScore) {
        return CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("https://game.thomasvitale.com"))
                .withType(gameScore.getClass().getTypeName())
                .withData(wrapCloudEventData(gameScore))
                .build();
    }

    private CloudEventData wrapCloudEventData(GameScore gameScore) {
        try {
            var gameScoreJson = objectMapper.writeValueAsString(gameScore);
            log.info("GameScore: {}", gameScoreJson);
            return JsonCloudEventData.wrap(objectMapper.readTree(gameScoreJson));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The GameScore object is not serializable to JSON.");
        }
    }

}
