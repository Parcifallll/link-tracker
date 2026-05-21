package backend.academy.linktracker.scrapper.service.senders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateInfo;
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
        when(kafkaProperties.getTopics()).thenReturn(topics);
        when(topics.getFallback()).thenReturn("unknown-updates");
        kafkaMessageSender = new KafkaMessageSender(kafkaTemplate, kafkaProperties);
    }

    @Test
    void sendUpdate_githubLink_sendsToGithubTopic() {
        // Arrange
        Link link = createLink("https://github.com/user/repo", 1L);
        List<Long> chatIds = List.of(123L, 456L);
        UpdateInfo updateInfo = new UpdateInfo("New Release", Map.of());

        // Act
        kafkaMessageSender.sendUpdate(link, updateInfo, chatIds);

        // Assert
        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(kafkaTemplate).send(eq("github-updates"), anyString(), captor.capture());

        LinkUpdate sent = captor.getValue();
        assertThat(sent.id()).isEqualTo(1L);
        assertThat(sent.url()).isEqualTo(URI.create("https://github.com/user/repo"));
        assertThat(sent.description()).isEqualTo("New Release");
        assertThat(sent.tgChatIds()).isEqualTo(chatIds);
    }

    @Test
    void sendUpdate_stackoverflowLink_sendsToStackoverflowTopic() {
        // Arrange
        Link link = createLink("https://stackoverflow.com/questions/123", 2L);
        List<Long> chatIds = List.of(789L);
        UpdateInfo updateInfo = new UpdateInfo("New Answer", Map.of());

        // Act
        kafkaMessageSender.sendUpdate(link, updateInfo, chatIds);

        // Assert
        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(kafkaTemplate).send(eq("stackoverflow-updates"), anyString(), captor.capture());

        LinkUpdate sent = captor.getValue();
        assertThat(sent.id()).isEqualTo(2L);
        assertThat(sent.url()).isEqualTo(URI.create("https://stackoverflow.com/questions/123"));
        assertThat(sent.description()).isEqualTo("New Answer");
        assertThat(sent.tgChatIds()).isEqualTo(chatIds);
    }

    @Test
    void sendError_githubLink_sendsErrorDescriptionToGithubTopic() {
        // Arrange
        Link link = createLink("https://github.com/user/repo", 1L);
        List<Long> chatIds = List.of(123L);
        String errorMessage = "Connection timeout";

        // Act
        kafkaMessageSender.sendError(link, errorMessage, chatIds);

        // Assert
        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(kafkaTemplate).send(eq("github-updates"), anyString(), captor.capture());

        LinkUpdate sent = captor.getValue();
        assertThat(sent.description()).contains("Error").contains("Connection timeout");
        assertThat(sent.tgChatIds()).isEqualTo(chatIds);
    }

    @Test
    void sendUpdate_unknownLink_sendsToFallbackTopic() {
        // Arrange
        Link link = createLink("https://unknown.com/page", 3L);
        List<Long> chatIds = List.of(123L);
        UpdateInfo updateInfo = new UpdateInfo("Update", Map.of());

        // Act
        kafkaMessageSender.sendUpdate(link, updateInfo, chatIds);

        // Assert
        ArgumentCaptor<LinkUpdate> captor = ArgumentCaptor.forClass(LinkUpdate.class);
        verify(kafkaTemplate).send(eq("unknown-updates"), anyString(), captor.capture());

        LinkUpdate sent = captor.getValue();
        assertThat(sent.id()).isEqualTo(3L);
        assertThat(sent.url()).isEqualTo(URI.create("https://unknown.com/page"));
        assertThat(sent.tgChatIds()).isEqualTo(chatIds);
    }

    private Link createLink(String url, Long id) {
        return new Link(id, URI.create(url), List.of(), List.of());
    }
}
