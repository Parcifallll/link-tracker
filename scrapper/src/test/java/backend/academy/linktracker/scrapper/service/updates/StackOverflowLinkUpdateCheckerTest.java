package backend.academy.linktracker.scrapper.service.updates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.Owner;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowAnswersResponse;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowCommentsResponse;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowResponse;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateInfo;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateType;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class StackOverflowLinkUpdateCheckerTest extends AbstractIntegrationTest {

    @Autowired
    StackOverflowLinkUpdateChecker checker;

    @MockitoBean
    StackOverflowClient stackOverflowClient;

    @MockitoBean
    StackoverflowProperties properties;

    @BeforeEach
    void setUp() {
        when(properties.getKey()).thenReturn("test-key");
    }

    @Test
    void newAnswer_returnsUpdateInfo() {
        Owner owner = new Owner("Expert");

        StackOverflowAnswersResponse.AnswerItem answer = new StackOverflowAnswersResponse.AnswerItem(
                1L, "This is the answer to your question", owner, 1714212000L);

        when(stackOverflowClient.getAnswers(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(new StackOverflowAnswersResponse(List.of(answer)));

        when(stackOverflowClient.getComments(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(new StackOverflowCommentsResponse(List.of()));

        StackOverflowResponse.QuestionItem question =
                new StackOverflowResponse.QuestionItem(123456L, "How to do X?", Instant.now());

        when(stackOverflowClient.getQuestion(anyLong(), anyString(), anyString()))
                .thenReturn(new StackOverflowResponse(List.of(question)));

        Link link = new Link(
                1L, URI.create("https://stackoverflow.com/questions/123456/how-to-do-x"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> resultOpt = checker.check(link);
        assertThat(resultOpt).isPresent();

        UpdateInfo result = resultOpt.orElseThrow();
        assertThat(result.linkTitle()).isEqualTo("How to do X?");
        assertThat(result.itemsByType()).containsKey(UpdateType.STACKOVERFLOW_ANSWER);
    }

    @Test
    void newComment_returnsUpdateInfo() {
        when(stackOverflowClient.getAnswers(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(new StackOverflowAnswersResponse(List.of()));

        Owner owner = new Owner("Commenter");

        StackOverflowCommentsResponse.CommentItem comment =
                new StackOverflowCommentsResponse.CommentItem(1L, "Great question!", owner, 1714212000L);

        when(stackOverflowClient.getComments(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(new StackOverflowCommentsResponse(List.of(comment)));

        StackOverflowResponse.QuestionItem question =
                new StackOverflowResponse.QuestionItem(123456L, "How to do X?", Instant.now());

        when(stackOverflowClient.getQuestion(anyLong(), anyString(), anyString()))
                .thenReturn(new StackOverflowResponse(List.of(question)));

        Link link = new Link(1L, URI.create("https://stackoverflow.com/questions/123456/title"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> resultOpt = checker.check(link);
        assertThat(resultOpt).isPresent();

        UpdateInfo result = resultOpt.orElseThrow();
        assertThat(result.itemsByType()).containsKey(UpdateType.STACKOVERFLOW_COMMENT);
    }

    @Test
    void previewTruncation_worksCorrectly() {
        String longBody = "a".repeat(300);

        Owner owner = new Owner("User");

        StackOverflowAnswersResponse.AnswerItem answer =
                new StackOverflowAnswersResponse.AnswerItem(1L, longBody, owner, 1714212000L);

        when(stackOverflowClient.getAnswers(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(new StackOverflowAnswersResponse(List.of(answer)));

        when(stackOverflowClient.getComments(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(new StackOverflowCommentsResponse(List.of()));

        StackOverflowResponse.QuestionItem question =
                new StackOverflowResponse.QuestionItem(123456L, "Test Question", Instant.now());

        when(stackOverflowClient.getQuestion(anyLong(), anyString(), anyString()))
                .thenReturn(new StackOverflowResponse(List.of(question)));

        Link link = new Link(1L, URI.create("https://stackoverflow.com/questions/123456/title"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> resultOpt = checker.check(link);
        assertThat(resultOpt).isPresent();

        UpdateInfo result = resultOpt.orElseThrow();
        List<UpdateInfo.UpdateItem> items = result.itemsByType().get(UpdateType.STACKOVERFLOW_ANSWER);
        assertThat(items.get(0).preview()).hasSizeLessThanOrEqualTo(200);
    }

    @Test
    void apiUnavailable_returnsEmpty() {
        when(stackOverflowClient.getAnswers(anyLong(), anyLong(), anyString(), anyString()))
                .thenThrow(new RuntimeException("API error"));

        Link link = new Link(1L, URI.create("https://stackoverflow.com/questions/123456/title"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);

        Optional<UpdateInfo> result = checker.check(link);
        assertThat(result).isEmpty();
    }

    @Test
    void noNewUpdates_returnsEmpty() {
        when(stackOverflowClient.getAnswers(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(new StackOverflowAnswersResponse(List.of()));

        when(stackOverflowClient.getComments(anyLong(), anyLong(), anyString(), anyString()))
                .thenReturn(new StackOverflowCommentsResponse(List.of()));

        Link link = new Link(1L, URI.create("https://stackoverflow.com/questions/123456/title"), List.of(), List.of());
        link.setLastCheckedAt(Instant.now());

        Optional<UpdateInfo> result = checker.check(link);
        assertThat(result).isEmpty();
    }
}
