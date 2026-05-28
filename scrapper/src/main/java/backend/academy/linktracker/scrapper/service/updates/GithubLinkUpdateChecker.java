package backend.academy.linktracker.scrapper.service.updates;

import backend.academy.linktracker.scrapper.client.github.GithubClient;
import backend.academy.linktracker.scrapper.client.github.dto.GithubIssue;
import backend.academy.linktracker.scrapper.client.github.dto.GithubPullRequest;
import backend.academy.linktracker.scrapper.configuration.ResilienceConfiguration;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateInfo;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GithubLinkUpdateChecker implements LinkUpdateChecker {

    private static final Logger log = LoggerFactory.getLogger(GithubLinkUpdateChecker.class);
    private static final String GITHUB_HOST = "github.com";

    private final GithubClient githubClient;

    @Override
    public boolean supports(Link link) {
        return link != null && link.getUrl() != null && link.getUrl().getHost().contains(GITHUB_HOST);
    }

    @Override
    @RateLimiter(name = ResilienceConfiguration.GITHUB)
    @Retry(name = ResilienceConfiguration.GITHUB)
    @CircuitBreaker(name = ResilienceConfiguration.GITHUB)
    public Optional<UpdateInfo> check(Link link) {
        try {
            String[] parts = link.getUrl().getPath().split("/");
            if (parts.length < 3) {
                return Optional.empty();
            }

            String owner = parts[1];
            String repo = parts[2];

            Instant lastChecked = link.getLastCheckedAt() != null ? link.getLastCheckedAt() : Instant.EPOCH;

            String since = formatSince(lastChecked);

            Map<UpdateType, List<UpdateInfo.UpdateItem>> itemsByType = new HashMap<>();

            // Issues
            List<GithubIssue> issues = githubClient.getIssues(owner, repo, since);
            if (!issues.isEmpty()) {
                List<UpdateInfo.UpdateItem> issueItems = issues.stream()
                        .map(issue -> new UpdateInfo.UpdateItem(
                                issue.title(), issue.user().login(), issue.createdAt(), truncate(issue.body(), 200)))
                        .toList();

                itemsByType.put(UpdateType.GITHUB_ISSUE, issueItems);
            }

            // PR
            List<GithubPullRequest> prs = githubClient.getPullRequests(owner, repo, since);
            if (!prs.isEmpty()) {
                List<UpdateInfo.UpdateItem> prItems = prs.stream()
                        .map(pr -> new UpdateInfo.UpdateItem(
                                pr.title(), pr.user().login(), pr.createdAt(), truncate(pr.body(), 200)))
                        .toList();

                itemsByType.put(UpdateType.GITHUB_PR, prItems);
            }

            if (itemsByType.isEmpty()) {
                return Optional.empty();
            }

            updateLastCheckedTime(link);

            String linkTitle = owner + "/" + repo;

            return Optional.of(new UpdateInfo(linkTitle, itemsByType));

        } catch (Exception e) {
            log.error("Failed to check GitHub updates for link: {}", link.getUrl(), e);
            return Optional.empty();
        }
    }

    private String formatSince(Instant lastChecked) {
        return DateTimeFormatter.ISO_INSTANT
                .withZone(ZoneOffset.UTC)
                .format(lastChecked.truncatedTo(ChronoUnit.SECONDS));
    }
}
