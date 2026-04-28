package backend.academy.linktracker.scrapper.service;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.client.github.GithubClient;
import backend.academy.linktracker.scrapper.model.Link;
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
@EnableWireMock({@ConfigureWireMock(name = "github-api", baseUrlProperties = "github.url")})
class GithubLinkUpdateCheckerTest {

    @Autowired
    GithubClient githubClient;

    GithubLinkUpdateChecker checker;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.github.token", () -> "test-token");
    }

    @BeforeEach
    void setUp() {
        checker = new GithubLinkUpdateChecker(githubClient);
    }

    @Test
    void newIssue_returnsUpdateInfo() {
        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
                .withQueryParam("since", matching(".*"))
                .withQueryParam("state", equalTo("all"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            [
                              {
                                "id": 1,
                                "title": "Fix critical bug",
                                "user": {"login": "johndoe"},
                                "body": "This is a critical bug that needs immediate attention",
                                "created_at": "2026-04-27T10:00:00Z"
                              }
                            ]
                            """)));

        stubFor(get(urlPathEqualTo("/repos/user/repo/pulls"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        link.setLastCheckedAt(Instant.parse("2026-04-20T00:00:00Z"));

        Optional<UpdateInfo> resultOpt = checker.check(link);
        assertThat(resultOpt).isPresent();

        UpdateInfo result = resultOpt.orElseThrow();

        assertThat(result.linkTitle()).isEqualTo("user/repo");
        assertThat(result.itemsByType()).containsKey(UpdateType.GITHUB_ISSUE);

        List<UpdateInfo.UpdateItem> issues = result.itemsByType().get(UpdateType.GITHUB_ISSUE);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).title()).isEqualTo("Fix critical bug");
        assertThat(issues.get(0).author()).isEqualTo("johndoe");
        assertThat(issues.get(0).preview()).isEqualTo("This is a critical bug that needs immediate attention");
    }

    @Test
    void newPullRequest_returnsUpdateInfo() {
        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        stubFor(get(urlPathEqualTo("/repos/user/repo/pulls"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            [
                              {
                                "id": 2,
                                "title": "Add new feature",
                                "user": {"login": "janedoe"},
                                "body": "This PR adds an amazing new feature",
                                "created_at": "2026-04-27T11:00:00Z"
                              }
                            ]
                            """)));

        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        link.setLastCheckedAt(Instant.parse("2026-04-20T00:00:00Z"));

        Optional<UpdateInfo> resultOpt = checker.check(link);
        assertThat(resultOpt).isPresent();

        UpdateInfo result = resultOpt.orElseThrow();

        assertThat(result.itemsByType()).containsKey(UpdateType.GITHUB_PR);

        List<UpdateInfo.UpdateItem> prs = result.itemsByType().get(UpdateType.GITHUB_PR);
        assertThat(prs).hasSize(1);
        assertThat(prs.get(0).title()).isEqualTo("Add new feature");
        assertThat(prs.get(0).author()).isEqualTo("janedoe");
    }

    @Test
    void previewLongerThan200Chars_truncated() {
        String longBody = "a".repeat(300);

        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format("""
                            [
                              {
                                "id": 1,
                                "title": "Test",
                                "user": {"login": "user"},
                                "body": "%s",
                                "created_at": "2026-04-27T10:00:00Z"
                              }
                            ]
                            """, longBody))));

        stubFor(get(urlPathEqualTo("/repos/user/repo/pulls"))
                .willReturn(aResponse().withStatus(200).withBody("[]")));

        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> resultOpt = checker.check(link);
        assertThat(resultOpt).isPresent();

        UpdateInfo result = resultOpt.orElseThrow();

        List<UpdateInfo.UpdateItem> items = result.itemsByType().get(UpdateType.GITHUB_ISSUE);
        assertThat(items.get(0).preview()).hasSize(200);
    }

    @Test
    void apiUnavailable_returnsEmpty() {
        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
                .willReturn(aResponse().withStatus(500)));

        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> result = checker.check(link);
        assertThat(result).isEmpty();
    }

    @Test
    void noNewUpdates_returnsEmpty() {
        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        stubFor(get(urlPathEqualTo("/repos/user/repo/pulls"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        link.setLastCheckedAt(Instant.now());

        Optional<UpdateInfo> result = checker.check(link);
        assertThat(result).isEmpty();
    }

    @Test
    void bothIssuesAndPRs_returnsBothTypes() {
        stubFor(get(urlPathEqualTo("/repos/user/repo/issues"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            [
                              {
                                "id": 1,
                                "title": "Issue 1",
                                "user": {"login": "user1"},
                                "body": "Issue body",
                                "created_at": "2026-04-27T10:00:00Z"
                              }
                            ]
                            """)));

        stubFor(get(urlPathEqualTo("/repos/user/repo/pulls"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            [
                              {
                                "id": 2,
                                "title": "PR 1",
                                "user": {"login": "user2"},
                                "body": "PR body",
                                "created_at": "2026-04-27T11:00:00Z"
                              }
                            ]
                            """)));

        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> resultOpt = checker.check(link);
        assertThat(resultOpt).isPresent();

        UpdateInfo result = resultOpt.orElseThrow();

        assertThat(result.itemsByType()).containsKeys(UpdateType.GITHUB_ISSUE, UpdateType.GITHUB_PR);
        assertThat(result.itemsByType().get(UpdateType.GITHUB_ISSUE)).hasSize(1);
        assertThat(result.itemsByType().get(UpdateType.GITHUB_PR)).hasSize(1);
    }
}
