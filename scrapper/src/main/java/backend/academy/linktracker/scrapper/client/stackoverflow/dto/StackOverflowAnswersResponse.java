package backend.academy.linktracker.scrapper.client.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record StackOverflowAnswersResponse(List<AnswerItem> items) {
    public record AnswerItem(
        @JsonProperty("answer_id") long answerId,
        String body,
        Owner owner,
        @JsonProperty("creation_date") long creationDate) {}
}
