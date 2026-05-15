package backend.academy.linktracker.scrapper.service.senders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "app.notification.type=kafka")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class KafkaMessageSenderIntegrationTest extends AbstractIntegrationTest {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Autowired
    private KafkaMessageSender kafkaMessageSender;

    @Autowired
    private KafkaProperties kafkaProperties;

    private KafkaConsumer<String, String> consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var consumerProps = Map.<String, Object>of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG,
                "test-group-" + System.currentTimeMillis(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        consumer = new KafkaConsumer<>(consumerProps);
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void sendUpdate_githubLink_messageAppearsInGithubUpdatesTopic() {
        // Arrange
        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        List<Long> chatIds = List.of(123L, 456L);
        UpdateInfo updateInfo = new UpdateInfo("New Release", Map.of());

        consumer.subscribe(List.of(kafkaProperties.getTopics().getGithubUpdates()));

        // Act
        kafkaMessageSender.sendUpdate(link, updateInfo, chatIds);

        // Assert
        List<ConsumerRecord<String, String>> received = new ArrayList<>();
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    consumer.poll(Duration.ofMillis(500)).forEach(received::add);
                    assertThat(received).map(ConsumerRecord::key).contains(String.valueOf(link.getId()));
                });

        LinkUpdate update = parseByKey(received, link.getId());
        assertThat(update.id()).isEqualTo(1L);
        assertThat(update.url()).isEqualTo(URI.create("https://github.com/user/repo"));
        assertThat(update.description()).isEqualTo("New Release");
        assertThat(update.tgChatIds()).isEqualTo(chatIds);
    }

    @Test
    void sendUpdate_stackoverflowLink_messageAppearsInStackoverflowUpdatesTopic() {
        // Arrange
        Link link = new Link(2L, URI.create("https://stackoverflow.com/questions/123"), List.of(), List.of());
        List<Long> chatIds = List.of(789L);
        UpdateInfo updateInfo = new UpdateInfo("New Answer", Map.of());

        consumer.subscribe(List.of(kafkaProperties.getTopics().getStackoverflowUpdates()));

        // Act
        kafkaMessageSender.sendUpdate(link, updateInfo, chatIds);

        // Assert
        List<ConsumerRecord<String, String>> received = new ArrayList<>();
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    consumer.poll(Duration.ofMillis(500)).forEach(received::add);
                    assertThat(received).map(ConsumerRecord::key).contains(String.valueOf(link.getId()));
                });

        LinkUpdate update = parseByKey(received, link.getId());
        assertThat(update.id()).isEqualTo(2L);
        assertThat(update.url()).isEqualTo(URI.create("https://stackoverflow.com/questions/123"));
        assertThat(update.description()).isEqualTo("New Answer");
        assertThat(update.tgChatIds()).isEqualTo(chatIds);
    }

    @Test
    void sendError_githubLink_errorMessageAppearsInTopic() {
        // Arrange
        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());
        List<Long> chatIds = List.of(123L);
        String errorMessage = "Connection timeout";

        consumer.subscribe(List.of(kafkaProperties.getTopics().getGithubUpdates()));

        // Act
        kafkaMessageSender.sendError(link, errorMessage, chatIds);

        // Assert
        List<ConsumerRecord<String, String>> received = new ArrayList<>();
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    consumer.poll(Duration.ofMillis(500)).forEach(received::add);
                    assertThat(received).map(ConsumerRecord::key).contains(String.valueOf(link.getId()));
                });

        LinkUpdate update = parseByKey(received, link.getId());
        assertThat(update.id()).isEqualTo(1L);
        assertThat(update.description()).contains("Error").contains("Connection timeout");
        assertThat(update.tgChatIds()).isEqualTo(chatIds);
    }

    private LinkUpdate parseByKey(List<ConsumerRecord<String, String>> records, long linkId) {
        return records.stream()
                .filter(r -> r.key().equals(String.valueOf(linkId)))
                .reduce((first, second) -> second)
                .map(r -> {
                    try {
                        return objectMapper.readValue(r.value(), LinkUpdate.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to deserialize LinkUpdate", e);
                    }
                })
                .orElseThrow(() -> new AssertionError("No record found for linkId=" + linkId));
    }
}
