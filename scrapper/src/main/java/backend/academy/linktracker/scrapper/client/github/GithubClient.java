package backend.academy.linktracker.scrapper.client.github;

import backend.academy.linktracker.scrapper.client.github.dto.GithubIssue;
import backend.academy.linktracker.scrapper.client.github.dto.GithubPullRequest;
import backend.academy.linktracker.scrapper.client.github.dto.GithubRepositoryResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import java.util.List;

@HttpExchange(url = "https://api.github.com")
public interface GithubClient {

    @GetExchange("/repos/{owner}/{repo}")
    GithubRepositoryResponse getRepository(
            @org.springframework.web.bind.annotation.PathVariable String owner,
            @org.springframework.web.bind.annotation.PathVariable String repo);

    @GetExchange("/repos/{owner}/{repo}/issues?since={since}&state=all")
    List<GithubIssue> getIssues(
        @PathVariable String owner,
        @PathVariable String repo,
        @PathVariable String since);

    @GetExchange("/repos/{owner}/{repo}/pulls?since={since}&state=all")
    List<GithubPullRequest> getPullRequests(
        @PathVariable String owner,
        @PathVariable String repo,
        @PathVariable String since);
}
