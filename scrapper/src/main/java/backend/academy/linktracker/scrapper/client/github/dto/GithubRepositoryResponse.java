package backend.academy.linktracker.scrapper.client.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record GithubRepositoryResponse(
    long id,
    String name,
    @JsonProperty("full_name") String fullName,
    @JsonProperty("updated_at") Instant updatedAt,
    @JsonProperty("pushed_at") Instant pushedAt
) {}
