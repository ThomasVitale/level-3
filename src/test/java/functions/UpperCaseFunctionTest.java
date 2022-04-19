package functions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(classes = CloudFunctionApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class UpperCaseFunctionTest {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  public void testUpperCase() throws Exception {
    webTestClient
      .post()
      .uri("/uppercase")
      .bodyValue("hello")
      .exchange()
      .expectStatus().isOk()
      .expectBody(String.class).isEqualTo("HELLO");
  }
}
