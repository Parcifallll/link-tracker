package backend.academy.linktracker.scrapper.client.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record GithubPullRequest(
        long id,
        String title,
        GithubUser user,
        String body,
        @JsonProperty("created_at") Instant createdAt) {}
