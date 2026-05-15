package backend.academy.linktracker.scrapper.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = "app.notification.type=kafka")
@Testcontainers
class KafkaMessageSenderIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.8.0")
    );

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaMessageSender kafkaMessageSender;

    @Autowired
    private KafkaProperties kafkaProperties;

    private KafkaConsumer<String, String> consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        var consumerProps = Map.<String, Object>of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
            ConsumerConfig.GROUP_ID_CONFIG, "test-group",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()
        );
        consumer = new KafkaConsumer<>(consumerProps);
        objectMapper = new ObjectMapper();
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
        await()
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> {
                var records = consumer.poll(Duration.ofMillis(100));
                assertEquals(1, records.count(), "Should receive one message");

                var record = records.iterator().next();
                LinkUpdate update = objectMapper.readValue(record.value(), LinkUpdate.class);
                assertEquals(1L, update.id());
                assertEquals(URI.create("https://github.com/user/repo"), update.url());
                assertEquals("New Release", update.description());
                assertEquals(chatIds, update.tgChatIds());
            });
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
        await()
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> {
                var records = consumer.poll(Duration.ofMillis(100));
                assertEquals(1, records.count(), "Should receive one message");

                var record = records.iterator().next();
                LinkUpdate update = objectMapper.readValue(record.value(), LinkUpdate.class);
                assertEquals(2L, update.id());
                assertEquals(URI.create("https://stackoverflow.com/questions/123"), update.url());
            });
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
        await()
            .atMost(Duration.ofSeconds(5))
            .pollInterval(Duration.ofMillis(100))
            .untilAsserted(() -> {
                var records = consumer.poll(Duration.ofMillis(100));
                assertEquals(1, records.count(), "Should receive one message");

                var record = records.iterator().next();
                LinkUpdate update = objectMapper.readValue(record.value(), LinkUpdate.class);
                assertEquals(1L, update.id());
                assertTrue(update.description().contains("Error"));
                assertTrue(update.description().contains("Connection timeout"));
            });
    }
}
