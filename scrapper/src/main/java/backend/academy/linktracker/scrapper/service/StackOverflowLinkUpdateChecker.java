package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowAnswersResponse;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowCommentsResponse;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowResponse;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import java.time.Instant;
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
public class StackOverflowLinkUpdateChecker implements LinkUpdateChecker {

    private static final Logger log = LoggerFactory.getLogger(StackOverflowLinkUpdateChecker.class);
    private static final String STACKOVERFLOW_HOST = "stackoverflow.com";

    private final StackOverflowClient stackOverflowClient;
    private final StackoverflowProperties stackoverflowProperties;

    @Override
    public boolean supports(Link link) {
        return link != null && link.getUrl() != null && link.getUrl().getHost().contains(STACKOVERFLOW_HOST);
    }

    @Override
    public Optional<UpdateInfo> check(Link link) {
        try {
            String[] parts = link.getUrl().getPath().split("/");
            if (parts.length < 3) {
                return Optional.empty();
            }

            long questionId = Long.parseLong(parts[2]);

            long fromDate =
                    link.getLastCheckedAt() != null ? link.getLastCheckedAt().getEpochSecond() : 0L;

            Map<UpdateType, List<UpdateInfo.UpdateItem>> itemsByType = new HashMap<>();

            StackOverflowAnswersResponse answersResponse = stackOverflowClient.getAnswers(
                    questionId, fromDate, "stackoverflow", stackoverflowProperties.getKey());

            if (!answersResponse.items().isEmpty()) {
                List<UpdateInfo.UpdateItem> answerItems = answersResponse.items().stream()
                        .map(answer -> new UpdateInfo.UpdateItem(
                                "New Answer",
                                answer.owner().displayName(),
                                Instant.ofEpochSecond(answer.creationDate()),
                                truncate(answer.body(), 200)))
                        .toList();

                itemsByType.put(UpdateType.STACKOVERFLOW_ANSWER, answerItems);
            }

            StackOverflowCommentsResponse commentsResponse = stackOverflowClient.getComments(
                    questionId, fromDate, "stackoverflow", stackoverflowProperties.getKey());

            if (!commentsResponse.items().isEmpty()) {
                List<UpdateInfo.UpdateItem> commentItems = commentsResponse.items().stream()
                        .map(comment -> new UpdateInfo.UpdateItem(
                                "New Comment",
                                comment.owner().displayName(),
                                Instant.ofEpochSecond(comment.creationDate()),
                                truncate(comment.body(), 200)))
                        .toList();

                itemsByType.put(UpdateType.STACKOVERFLOW_COMMENT, commentItems);
            }

            if (itemsByType.isEmpty()) {
                return Optional.empty();
            }

            updateLastCheckedTime(link);

            String questionTitle = getQuestionTitle(questionId);

            return Optional.of(new UpdateInfo(questionTitle, itemsByType));

        } catch (Exception e) {
            log.error("Failed to check StackOverflow updates for link: {}", link.getUrl(), e);
            return Optional.empty();
        }
    }

    private String getQuestionTitle(long questionId) {
        try {
            return stackOverflowClient
                    .getQuestion(questionId, "stackoverflow", stackoverflowProperties.getKey())
                    .items()
                    .stream()
                    .findFirst()
                    .map(StackOverflowResponse.QuestionItem::title)
                    .orElse("Question #" + questionId);
        } catch (Exception e) {
            log.warn("Failed to get question title for id: {}", questionId, e);
            return "Question #" + questionId;
        }
    }
}
