package functions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;

@SpringBootApplication
public class CloudFunctionApplication {

  private static final String LEVEL_NAME = "level-3";
  private final ObjectMapper objectMapper;

  public CloudFunctionApplication(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public static void main(String[] args) {
    SpringApplication.run(CloudFunctionApplication.class, args);
  }

  @Bean
  public Function<Mono<Answers>, Mono<GameScore>> answers(StringRedisTemplate redisTemplate) {
    return answers -> answers
            .map(this::scoreLevel)
            .map(tuple -> {
              redisTemplate.opsForList().rightPush("score-" + tuple.getT1().sessionId(), writeValueAsString(tuple.getT1()));
              redisTemplate.opsForList().rightPush(tuple.getT2().gameTimeId(), writeValueAsString(tuple.getT2()));
              return tuple.getT1();
            });
  }

  private Tuple2<GameScore,GameTime> scoreLevel(Answers answers) {
      System.out.println("Answers Counter in Level 3: " + answers.counter());
    int points = Objects.nonNull(answers.counter()) ? answers.counter() : -1;
    var score = new GameScore(answers.sessionId(), LocalDateTime.now(), LEVEL_NAME, points);
    var gameTime = new GameTime("time-" + score.sessionId(), score.sessionId(), score.level(), "end", score.gameTime());
    return Tuples.of(score, gameTime);
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

record GameScore(@JsonProperty("SessionId")
                 String sessionId,
                 @JsonProperty("Time")
                 LocalDateTime gameTime,
                 @JsonProperty("Level")
                 String level,
                 @JsonProperty("LevelScore")
                 int levelScore){}

record GameTime(String gameTimeId, String sessionId, String level, String type, LocalDateTime time){}
