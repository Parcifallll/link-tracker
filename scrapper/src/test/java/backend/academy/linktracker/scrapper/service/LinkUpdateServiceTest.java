package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.github.GithubClient;
import backend.academy.linktracker.scrapper.client.github.dto.GithubRepositoryResponse;
import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowResponse;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkUpdateServiceTest {

    @Mock
    GithubClient githubClient;

    @Mock
    StackOverflowClient stackOverflowClient;

    @Mock
    StackoverflowProperties stackoverflowProperties;

    LinkUpdateService linkUpdateService;

    @BeforeEach
    void setUp() {
        linkUpdateService = new LinkUpdateService(githubClient, stackOverflowClient, stackoverflowProperties);
    }

    private Link githubLink() {
        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);
        return link;
    }

    private Link stackoverflowLink() {
        Link link =
                new Link(2L, URI.create("https://stackoverflow.com/questions/11227809/title"), List.of(), List.of());
        link.setLastCheckedAt(Instant.EPOCH);
        return link;
    }

    private GithubRepositoryResponse githubResponse(Instant updatedAt) {
        return new GithubRepositoryResponse(1L, "repo", "user/repo", updatedAt, updatedAt);
    }

    private StackOverflowResponse soResponse(Instant lastActivity) {
        return new StackOverflowResponse(
                List.of(new StackOverflowResponse.QuestionItem(11227809L, "title", lastActivity)));
    }

    @Test
    void github_hasUpdate_returnsTrue() {
        Link link = githubLink();
        when(githubClient.getRepository("user", "repo")).thenReturn(githubResponse(Instant.now()));
        assertTrue(linkUpdateService.hasUpdate(link));
    }

    @Test
    void github_noUpdate_returnsFalse() {
        Link link = githubLink();
        link.setLastCheckedAt(Instant.now());
        when(githubClient.getRepository("user", "repo")).thenReturn(githubResponse(Instant.EPOCH));
        assertFalse(linkUpdateService.hasUpdate(link));
    }

    @Test
    void github_clientThrowsException_returnsFalse() {
        Link link = githubLink();
        when(githubClient.getRepository("user", "repo")).thenThrow(new RuntimeException("500 Internal Server Error"));
        assertFalse(linkUpdateService.hasUpdate(link));
    }

    @Test
    void stackoverflow_hasUpdate_returnsTrue() {
        Link link = stackoverflowLink();
        when(stackoverflowProperties.getKey()).thenReturn("test-key");
        when(stackOverflowClient.getQuestion(11227809L, "stackoverflow", "test-key"))
                .thenReturn(soResponse(Instant.now()));
        assertTrue(linkUpdateService.hasUpdate(link));
    }

    @Test
    void stackoverflow_noUpdate_returnsFalse() {
        Link link = stackoverflowLink();
        link.setLastCheckedAt(Instant.now());
        when(stackoverflowProperties.getKey()).thenReturn("test-key");
        when(stackOverflowClient.getQuestion(11227809L, "stackoverflow", "test-key"))
                .thenReturn(soResponse(Instant.EPOCH));
        assertFalse(linkUpdateService.hasUpdate(link));
    }

    @Test
    void stackoverflow_clientThrowsException_returnsFalse() {
        Link link = stackoverflowLink();
        when(stackoverflowProperties.getKey()).thenReturn("test-key");
        when(stackOverflowClient.getQuestion(11227809L, "stackoverflow", "test-key"))
                .thenThrow(new RuntimeException("400 Bad Request"));
        assertFalse(linkUpdateService.hasUpdate(link));
    }

    @Test
    void unknownHost_returnsFalse() {
        Link link = new Link(3L, URI.create("https://unknown.com/some/path"), List.of(), List.of());
        assertFalse(linkUpdateService.hasUpdate(link));
    }
}
