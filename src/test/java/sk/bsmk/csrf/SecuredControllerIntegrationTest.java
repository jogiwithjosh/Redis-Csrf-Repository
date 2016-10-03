package sk.bsmk.csrf;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;
import redis.embedded.RedisServer;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@IntegrationTest
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = WebSecurityConfig.class)
@WebAppConfiguration
public class SecuredControllerIntegrationTest {

    private static final String URL = "http://localhost:8080/";

    private RedisServer redisServer;

    private static final RestTemplate anonymous = new TestRestTemplate();
    private static final RestTemplate user = new TestRestTemplate("user", "password");

    @Before
    public void setup() throws Exception {
        redisServer = new RedisServer(6379);
        redisServer.start();
    }

    @After
    public void tearDown() throws Exception {
        redisServer.stop();
    }

    @Test
    public void thatInfoIsAccessible() {
        ResponseEntity<String> response = anonymous.getForEntity(URL + "info", String.class);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(SecuredController.DEFAULT_INFO));
    }

    @Test
    public void thatLoginIsInaccessibleWithoutCredentials() {
        ResponseEntity<String> response = anonymous.getForEntity(URL + "login", String.class);
        assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DirtiesContext
    public void thatLoginIsAccessibleWithCredentials() {
        ResponseEntity<String> response = user.getForEntity(URL + "login", String.class);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is(SecuredController.LOGGED_MESSAGE));
        assertThat(response.getHeaders(), Matchers.hasKey(RedisCsrfTokenRepository.CSRF_HEADER_NAME));
    }

    @Test
    @DirtiesContext
    public void thatUpdateInfoIsInaccessibleWithoutCsrfToken() {
        ResponseEntity<String> putResponse = user.exchange(URL + "info", HttpMethod.PUT, null, String.class);
        assertThat(putResponse.getStatusCode(), is(HttpStatus.FORBIDDEN));
        assertThat(putResponse.getBody(), containsString("Expected CSRF token not found"));

        ResponseEntity<String> infoResponse = anonymous.getForEntity(URL + "info", String.class);
        assertThat(infoResponse.getBody(), is(SecuredController.DEFAULT_INFO));
    }

    @Test
    @DirtiesContext
    public void thatUpdateInfoIsInaccessibleWithCsrfTokenAndNoCredentials() {
        ResponseEntity<String> loginResponse = anonymous.getForEntity(URL + "login", String.class);
        String csrfToken = loginResponse.getHeaders().getFirst(RedisCsrfTokenRepository.CSRF_HEADER_NAME);

        HttpHeaders headers = new HttpHeaders();
        headers.add(RedisCsrfTokenRepository.CSRF_HEADER_NAME, csrfToken);

        final String newInfo = "Some new info with csrf";

        ResponseEntity<String> response = anonymous.exchange(URL + "info", HttpMethod.PUT, new HttpEntity<>(newInfo, headers), String.class);
        assertThat(response.getStatusCode(), is(HttpStatus.FORBIDDEN));

        ResponseEntity<String> infoResponse = anonymous.getForEntity(URL + "info", String.class);
        assertThat(infoResponse.getBody(), is(SecuredController.DEFAULT_INFO));
    }

    @Test
    @DirtiesContext
    public void thatUpdateInfoIsAccessibleWithCsrfTokenAndCredentials() {
        ResponseEntity<String> loginResponse = user.getForEntity(URL + "login", String.class);
        String csrfToken = loginResponse.getHeaders().getFirst(RedisCsrfTokenRepository.CSRF_HEADER_NAME);

        HttpHeaders headers = new HttpHeaders();
        headers.add(RedisCsrfTokenRepository.CSRF_HEADER_NAME, csrfToken);

        final String newInfo = "Some new info with csrf";
        ResponseEntity<String> response = user.exchange(URL + "info", HttpMethod.PUT, new HttpEntity<>(newInfo, headers), String.class);
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getBody(), is("info updated"));

        ResponseEntity<String> infoResponse = anonymous.getForEntity(URL + "info", String.class);
        assertThat(infoResponse.getBody(), is(newInfo));
    }

}