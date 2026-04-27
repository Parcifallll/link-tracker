package backend.academy.linktracker.scrapper.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest
@EnableWireMock({@ConfigureWireMock(name = "stackoverflow-api", baseUrlProperties = "stackoverflow.url")})
class StackOverflowLinkUpdateCheckerTest {

    @Autowired
    StackOverflowClient stackOverflowClient;

    @Autowired
    StackoverflowProperties properties;

    StackOverflowLinkUpdateChecker checker;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.stackoverflow.key", () -> "test-key");
        registry.add("app.stackoverflow.access-token", () -> "test-token");
    }

    @BeforeEach
    void setUp() {
        checker = new StackOverflowLinkUpdateChecker(stackOverflowClient, properties);
    }

    @Test
    void newAnswer_returnsUpdateInfo() {
        stubFor(get(urlPathEqualTo("/questions/123456/answers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "items": [
                                {
                                  "answer_id": 1,
                                  "body": "This is the answer to your question",
                                  "owner": {"display_name": "Expert"},
                                  "creation_date": 1714212000
                                }
                              ]
                            }
                            """)));

        stubFor(get(urlPathEqualTo("/questions/123456/comments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"items\": []}")));

        stubFor(get(urlPathEqualTo("/questions/123456"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "items": [
                                {
                                  "question_id": 123456,
                                  "title": "How to do X?",
                                  "last_activity_date": "2026-04-27T10:00:00Z"
                                }
                              ]
                            }
                            """)));

        Link link = new Link(1L, URI.create("https://stackoverflow.com/questions/123456/title"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> resultOpt = checker.check(link);
        assertThat(resultOpt).isPresent();

        UpdateInfo result = resultOpt.orElseThrow();

        assertThat(result.linkTitle()).isEqualTo("How to do X?");
        assertThat(result.itemsByType()).containsKey(UpdateType.STACKOVERFLOW_ANSWER);

        List<UpdateInfo.UpdateItem> answers = result.itemsByType().get(UpdateType.STACKOVERFLOW_ANSWER);
        assertThat(answers).hasSize(1);
        assertThat(answers.get(0).author()).isEqualTo("Expert");
        assertThat(answers.get(0).preview()).isEqualTo("This is the answer to your question");
    }

    @Test
    void newComment_returnsUpdateInfo() {
        stubFor(get(urlPathEqualTo("/questions/123456/answers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"items\": []}")));

        stubFor(get(urlPathEqualTo("/questions/123456/comments"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "items": [
                                {
                                  "comment_id": 1,
                                  "body": "Great question!",
                                  "owner": {"display_name": "Commenter"},
                                  "creation_date": 1714212000
                                }
                              ]
                            }
                            """)));

        stubFor(get(urlPathEqualTo("/questions/123456"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "items": [
                                {
                                  "question_id": 123456,
                                  "title": "How to do X?",
                                  "last_activity_date": "2026-04-27T10:00:00Z"
                                }
                              ]
                            }
                            """)));

        Link link = new Link(1L, URI.create("https://stackoverflow.com/questions/123456/title"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> resultOpt = checker.check(link);
        assertThat(resultOpt).isPresent();

        UpdateInfo result = resultOpt.orElseThrow();

        assertThat(result.itemsByType()).containsKey(UpdateType.STACKOVERFLOW_COMMENT);
    }

    @Test
    void apiUnavailable_returnsEmpty() {
        stubFor(get(urlPathEqualTo("/questions/123456/answers"))
                .willReturn(aResponse().withStatus(500)));

        Link link = new Link(1L, URI.create("https://stackoverflow.com/questions/123456/title"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> result = checker.check(link);
        assertThat(result).isEmpty();
    }

    @Test
    void previewTruncation_worksCorrectly() {
        String longBody = "a".repeat(300);

        stubFor(get(urlPathEqualTo("/questions/123456/answers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                            {
                              "items": [
                                {
                                  "answer_id": 1,
                                  "body": "%s",
                                  "owner": {"display_name": "User"},
                                  "creation_date": 1714212000
                                }
                              ]
                            }
                            """, longBody))));

        stubFor(get(urlPathEqualTo("/questions/123456/comments"))
                .willReturn(aResponse().withStatus(200).withBody("{\"items\": []}")));

        stubFor(get(urlPathEqualTo("/questions/123456"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "items": [
                                {
                                  "question_id": 123456,
                                  "title": "Test",
                                  "last_activity_date": "2026-04-27T10:00:00Z"
                                }
                              ]
                            }
                            """)));

        Link link = new Link(1L, URI.create("https://stackoverflow.com/questions/123456/title"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> resultOpt = checker.check(link);
        assertThat(resultOpt).isPresent();

        UpdateInfo result = resultOpt.orElseThrow();

        List<UpdateInfo.UpdateItem> items = result.itemsByType().get(UpdateType.STACKOVERFLOW_ANSWER);
        assertThat(items.get(0).preview()).hasSize(200);
    }
}
