package functions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.TypeAccess;
import org.springframework.nativex.hint.TypeHint;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.function.Function;

@SpringBootApplication
@NativeHint(trigger = NioSocketChannel.class, types = {
        @TypeHint(types = NioSocketChannel.class, access = {TypeAccess.DECLARED_CONSTRUCTORS, TypeAccess.DECLARED_METHODS}),
        @TypeHint(types = NioDatagramChannel.class)
})
public class Level3Application {

    private static final Logger log = LoggerFactory.getLogger(Level3Application.class);
    private static final String LEVEL_NAME = "level-3";
    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;

    public Level3Application(ObjectMapper objectMapper, ReactiveStringRedisTemplate redisTemplate) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(Level3Application.class, args);
    }

    @Bean
    public Function<Mono<Answers>, Mono<GameScore>> answers() {
        return answers -> answers
                .map(this::scoreLevel)
                .flatMap(this::processLevel);
    }

    private Tuple2<GameScore,GameTime> scoreLevel(Answers answers) {
        log.debug("Answers counter in Level 3: {}", answers.counter());
        int points = answers.counter() > 1 ? answers.counter() : -1;
        var gameScore = new GameScore(answers.sessionId(), LocalDateTime.now(), LEVEL_NAME, points);
        var gameTime = new GameTime("time-" + gameScore.sessionId(), gameScore.sessionId(), gameScore.level(), "end", gameScore.gameTime());
        return Tuples.of(gameScore, gameTime);
    }

    private Mono<GameScore> processLevel(Tuple2<GameScore,GameTime> tuple) {
        return redisTemplate.opsForList().rightPush("score-" + tuple.getT1().sessionId(), writeValueAsString(tuple.getT1()))
                .then(redisTemplate.opsForList().rightPush(tuple.getT2().gameTimeId(), writeValueAsString(tuple.getT2())))
                .then(Mono.just(tuple.getT1()));
    }

    private String writeValueAsString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Error when serializing Score", ex);
        }
    }

}

record Answers(String sessionId, int counter){}

record GameScore(
    @JsonProperty("SessionId")
    String sessionId,
    @JsonProperty("Time")
    LocalDateTime gameTime,
    @JsonProperty("Level")
    String level,
    @JsonProperty("LevelScore")
    int levelScore
){}

record GameTime(
    @JsonProperty("GameTimeId")
    String gameTimeId,
    @JsonProperty("SessionId")
    String sessionId,
    @JsonProperty("Level")
    String level,
    @JsonProperty("Type")
    String type,
    @JsonProperty("Time")
    LocalDateTime time
){}
