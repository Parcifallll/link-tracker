package backend.academy.linktracker.scrapper.client.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StackOverflowCommentsResponse(List<CommentItem> items) {
    public record CommentItem(
            @JsonProperty("comment_id") long commentId,
            String body,
            Owner owner,
            @JsonProperty("creation_date") long creationDate) {}
}
