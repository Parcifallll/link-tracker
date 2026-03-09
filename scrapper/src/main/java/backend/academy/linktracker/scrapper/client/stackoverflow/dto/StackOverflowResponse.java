package backend.academy.linktracker.scrapper.client.stackoverflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record StackOverflowResponse(List<QuestionItem> items) {
    public record QuestionItem(
            @JsonProperty("question_id") long questionId,
            String title,
            @JsonProperty("last_activity_date") Instant lastActivityDate) {}
}
