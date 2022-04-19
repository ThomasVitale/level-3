package functions;

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
  public Function<Mono<Questions>, Mono<Score>> question(StringRedisTemplate redisTemplate) {
    return questions -> questions
            .map(this::playLevel)
            .map(tuple -> {
              redisTemplate.opsForValue().set("score-" + tuple.getT1().sessionId(), writeValueAsString(tuple.getT1()));
              redisTemplate.opsForValue().set(tuple.getT2().gameTimeId(), writeValueAsString(tuple.getT2()));
              return tuple.getT1();
            });
  }

  private Tuple2<Score,GameTime> playLevel(Questions questions) {
    int points = Objects.nonNull(questions.question1()) ? 10 : -1;
    var score = new Score(questions.sessionId(), LocalDateTime.now(), LEVEL_NAME, points);
    var gameTime = new GameTime("time-" + score.sessionId(), score.sessionId(), score.level(), "end", score.time());
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

record Questions(String sessionId, String question1){}

record Score(String sessionId, LocalDateTime time, String level, int levelScore){}

record GameTime(String gameTimeId, String sessionId, String level, String type, LocalDateTime time){}
