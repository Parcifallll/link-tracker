package backend.academy.linktracker.scrapper.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaMessageSenderTest {

    @Mock
    private KafkaTemplate<String, LinkUpdate> kafkaTemplate;

    @Mock
    private KafkaProperties kafkaProperties;

    @Mock
    private KafkaProperties.Topics topics;

    private KafkaMessageSender kafkaMessageSender;

    @BeforeEach
    void setUp() {
        kafkaMessageSender = new KafkaMessageSender(kafkaTemplate, kafkaProperties);
    }

    @Test
    void testSendUpdateForGithubLink() {
        // Arrange
        Link link = createLink("https://github.com/user/repo", 1L);
        List<Long> chatIds = List.of(123L, 456L);
        UpdateInfo updateInfo = new UpdateInfo("New Release", Map.of());

        when(kafkaProperties.getTopics()).thenReturn(topics);
        when(topics.getGithubUpdates()).thenReturn("github-updates");

        // Act
        kafkaMessageSender.sendUpdate(link, updateInfo, chatIds);

        // Assert
        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(kafkaTemplate).send(eq("github-updates"), anyString(), captor.capture());

        LinkUpdate sentUpdate = captor.getValue();
        assert sentUpdate.id() == 1L;
        assert sentUpdate.url().equals(URI.create("https://github.com/user/repo"));
        assert sentUpdate.description().equals("New Release");
        assert sentUpdate.tgChatIds().equals(chatIds);
    }

    @Test
    void testSendUpdateForStackOverflowLink() {
        // Arrange
        Link link = createLink("https://stackoverflow.com/questions/123", 2L);
        List<Long> chatIds = List.of(789L);
        UpdateInfo updateInfo = new UpdateInfo("New Answer", Map.of());

        when(kafkaProperties.getTopics()).thenReturn(topics);
        when(topics.getStackoverflowUpdates()).thenReturn("stackoverflow-updates");

        // Act
        kafkaMessageSender.sendUpdate(link, updateInfo, chatIds);

        // Assert
        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(kafkaTemplate).send(eq("stackoverflow-updates"), anyString(), captor.capture());

        LinkUpdate sentUpdate = captor.getValue();
        assert sentUpdate.id() == 2L;
        assert sentUpdate.url().equals(URI.create("https://stackoverflow.com/questions/123"));
    }

    @Test
    void testSendErrorForGithubLink() {
        // Arrange
        Link link = createLink("https://github.com/user/repo", 1L);
        List<Long> chatIds = List.of(123L);
        String errorMessage = "Connection timeout";

        when(kafkaProperties.getTopics()).thenReturn(topics);
        when(topics.getGithubUpdates()).thenReturn("github-updates");

        // Act
        kafkaMessageSender.sendError(link, errorMessage, chatIds);

        // Assert
        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(kafkaTemplate).send(eq("github-updates"), anyString(), captor.capture());

        LinkUpdate sentUpdate = captor.getValue();
        assert sentUpdate.description().contains("Error");
        assert sentUpdate.description().contains("Connection timeout");
    }

    @Test
    void testSendUpdateUseFallbackTopicForUnknownSource() {
        // Arrange
        Link link = createLink("https://unknown.com/page", 1L);
        List<Long> chatIds = List.of(123L);
        UpdateInfo updateInfo = new UpdateInfo("Update", Map.of());

        when(kafkaProperties.getTopics()).thenReturn(topics);
        when(topics.getFallback()).thenReturn("unknown-updates");

        // Act
        kafkaMessageSender.sendUpdate(link, updateInfo, chatIds);

        // Assert
        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(kafkaTemplate).send(eq("unknown-updates"), anyString(), captor.capture());

        LinkUpdate sentUpdate = captor.getValue();
        assert sentUpdate.id() == 1L;
        assert sentUpdate.url().equals(URI.create("https://unknown.com/page"));
    }

    private Link createLink(String url, Long id) {
        return new Link(id, URI.create(url), List.of(), List.of());
    }
}

