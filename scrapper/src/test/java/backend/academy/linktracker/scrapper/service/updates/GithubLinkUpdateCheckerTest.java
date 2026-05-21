package backend.academy.linktracker.scrapper.service.updates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import backend.academy.linktracker.scrapper.client.github.GithubClient;
import backend.academy.linktracker.scrapper.client.github.dto.GithubIssue;
import backend.academy.linktracker.scrapper.client.github.dto.GithubPullRequest;
import backend.academy.linktracker.scrapper.client.github.dto.GithubUser;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateInfo;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateType;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GithubLinkUpdateCheckerTest extends AbstractIntegrationTest {

    @Autowired
    GithubLinkUpdateChecker checker;

    @MockitoBean
    GithubClient githubClient;

    @BeforeEach
    void setUp() {
        // checker будет получать замоканный клиент через Spring
    }

    @Test
    void newIssue_returnsUpdateInfo() {
        when(githubClient.getIssues(eq("user"), eq("repo"), anyString()))
                .thenReturn(List.of(new GithubIssue(
                        1L,
                        "Fix critical bug",
                        new GithubUser("johndoe"),
                        "This is a critical bug that needs immediate attention",
                        Instant.parse("2026-04-27T10:00:00Z"))));

        when(githubClient.getPullRequests(eq("user"), eq("repo"), anyString())).thenReturn(List.of());

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
    }

    @Test
    void newPullRequest_returnsUpdateInfo() {
        when(githubClient.getIssues(eq("user"), eq("repo"), anyString())).thenReturn(List.of());

        when(githubClient.getPullRequests(eq("user"), eq("repo"), anyString()))
                .thenReturn(List.of(new GithubPullRequest(
                        2L,
                        "Add new feature",
                        new GithubUser("janedoe"),
                        "This PR adds an amazing new feature",
                        Instant.parse("2026-04-27T11:00:00Z"))));

        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        link.setLastCheckedAt(Instant.parse("2026-04-20T00:00:00Z"));

        Optional<UpdateInfo> resultOpt = checker.check(link);
        assertThat(resultOpt).isPresent();

        UpdateInfo result = resultOpt.orElseThrow();
        assertThat(result.itemsByType()).containsKey(UpdateType.GITHUB_PR);
    }

    @Test
    void previewLongerThan200Chars_truncated() {
        String longBody = "a".repeat(300);

        when(githubClient.getIssues(eq("user"), eq("repo"), anyString()))
                .thenReturn(List.of(new GithubIssue(
                        1L, "Test", new GithubUser("user"), longBody, Instant.parse("2026-04-27T10:00:00Z"))));

        when(githubClient.getPullRequests(eq("user"), eq("repo"), anyString())).thenReturn(List.of());

        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> resultOpt = checker.check(link);
        assertThat(resultOpt).isPresent();

        UpdateInfo result = resultOpt.orElseThrow();
        List<UpdateInfo.UpdateItem> items = result.itemsByType().get(UpdateType.GITHUB_ISSUE);
        assertThat(items.get(0).preview()).hasSizeLessThanOrEqualTo(200);
    }

    @Test
    void bothIssuesAndPRs_returnsBothTypes() {
        when(githubClient.getIssues(eq("user"), eq("repo"), anyString()))
                .thenReturn(List.of(new GithubIssue(
                        1L, "Issue 1", new GithubUser("user1"), "Issue body", Instant.parse("2026-04-27T10:00:00Z"))));

        when(githubClient.getPullRequests(eq("user"), eq("repo"), anyString()))
                .thenReturn(List.of(new GithubPullRequest(
                        2L, "PR 1", new GithubUser("user2"), "PR body", Instant.parse("2026-04-27T11:00:00Z"))));

        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> resultOpt = checker.check(link);
        assertThat(resultOpt).isPresent();

        UpdateInfo result = resultOpt.orElseThrow();
        assertThat(result.itemsByType()).containsKeys(UpdateType.GITHUB_ISSUE, UpdateType.GITHUB_PR);
    }

    @Test
    void apiUnavailable_returnsEmpty() {
        when(githubClient.getIssues(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API unavailable"));

        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> result = checker.check(link);
        assertThat(result).isEmpty();
    }

    @Test
    void noNewUpdates_returnsEmpty() {
        when(githubClient.getIssues(anyString(), anyString(), anyString())).thenReturn(List.of());

        when(githubClient.getPullRequests(anyString(), anyString(), anyString()))
                .thenReturn(List.of());

        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        link.setLastCheckedAt(Instant.now());

        Optional<UpdateInfo> result = checker.check(link);
        assertThat(result).isEmpty();
    }
}
