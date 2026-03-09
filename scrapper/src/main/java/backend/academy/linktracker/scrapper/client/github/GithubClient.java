package backend.academy.linktracker.scrapper.client.github;

import backend.academy.linktracker.scrapper.client.github.dto.GithubRepositoryResponse;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "https://api.github.com")
public interface GithubClient {

    @GetExchange("/repos/{owner}/{repo}")
    GithubRepositoryResponse getRepository(
            @org.springframework.web.bind.annotation.PathVariable String owner,
            @org.springframework.web.bind.annotation.PathVariable String repo);
}
