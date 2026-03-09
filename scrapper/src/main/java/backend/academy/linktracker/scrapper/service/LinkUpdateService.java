package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.github.GithubClient;
import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkUpdateService {

    private static final String GITHUB_HOST = "github.com";
    private static final String STACKOVERFLOW_HOST = "stackoverflow.com";

    private final GithubClient githubClient;
    private final StackOverflowClient stackOverflowClient;
    private final StackoverflowProperties stackoverflowProperties;

    public boolean hasUpdate(Link link) {
        String host = link.getUrl().getHost();
        MDC.put("url", link.getUrl().toString());
        try {
            if (host.contains(GITHUB_HOST)) {
                return checkGithub(link);
            } else if (host.contains(STACKOVERFLOW_HOST)) {
                return checkStackOverflow(link);
            }
            return false;
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("Failed to check link update");
            return false;
        } finally {
            MDC.clear();
        }
    }

    private boolean checkGithub(Link link) {
        // path: /owner/repo
        String[] parts = link.getUrl().getPath().split("/");
        if (parts.length < 3) {
            return false;
        }
        String owner = parts[1];
        String repo = parts[2];

        Instant updatedAt = githubClient.getRepository(owner, repo).updatedAt();
        boolean hasUpdate = updatedAt.isAfter(link.getLastCheckedAt());

        if (hasUpdate) {
            link.setLastCheckedAt(Instant.now());
        }
        return hasUpdate;
    }

    private boolean checkStackOverflow(Link link) {
        // path: /questions/{id}/...
        String[] parts = link.getUrl().getPath().split("/");
        if (parts.length < 3) {
            return false;
        }
        long questionId = Long.parseLong(parts[2]);

        Instant lastActivity =
                stackOverflowClient
                        .getQuestion(questionId, "stackoverflow", stackoverflowProperties.getKey())
                        .items()
                        .stream()
                        .findFirst()
                        .map(item -> item.lastActivityDate())
                        .orElse(Instant.MIN);

        boolean hasUpdate = lastActivity.isAfter(link.getLastCheckedAt());

        if (hasUpdate) {
            link.setLastCheckedAt(Instant.now());
        }
        return hasUpdate;
    }
}
