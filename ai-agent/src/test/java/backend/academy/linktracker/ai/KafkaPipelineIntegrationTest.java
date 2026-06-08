package backend.academy.linktracker.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.ai.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.ai.dto.RawLinkUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class KafkaPipelineIntegrationTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.1"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoSpyBean
    private KafkaTemplate<String, ProcessedLinkUpdate> kafkaTemplate;

    private KafkaProducer<String, String> rawProducer;
    private KafkaConsumer<String, String> processedConsumer;

    @BeforeEach
    void setUp() {
        rawProducer = new KafkaProducer<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class));

        processedConsumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "test-processed-consumer",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class));

        processedConsumer.subscribe(List.of("link.processed-updates"));
    }

    @AfterEach
    void tearDown() {
        rawProducer.close();
        processedConsumer.close();
    }

    @Test
    void validMessage_receivedAndPublishedToProcessedTopic() throws Exception {
        RawLinkUpdate update = new RawLinkUpdate(
                42L,
                "New pull request opened: Fix memory leak in the connection pool handler",
                "alice",
                List.of(100L, 200L));

        rawProducer.send(new ProducerRecord<>("link.raw-updates", "42", objectMapper.writeValueAsString(update)));
        rawProducer.flush();

        verify(kafkaTemplate, timeout(10_000)).send(anyString(), anyString(), any(ProcessedLinkUpdate.class));

        List<ProcessedLinkUpdate> received = pollMessages(Duration.ofSeconds(10));
        assertThat(received).hasSize(1);
        assertThat(received.get(0).id()).isEqualTo(42L);
        assertThat(received.get(0).tgChatIds()).containsExactlyInAnyOrder(100L, 200L);
        assertThat(received.get(0).priority()).isEqualTo("HIGH");
    }

    @Test
    void filteredMessage_notPublished() throws Exception {
        RawLinkUpdate update = new RawLinkUpdate(99L, "spam offer check this out now buy now", "alice", List.of(100L));

        rawProducer.send(new ProducerRecord<>("link.raw-updates", "99", objectMapper.writeValueAsString(update)));
        rawProducer.flush();

        await().atMost(8, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> verify(kafkaTemplate, never()).send(anyString(), anyString(), any()));
    }

    @Test
    void malformedMessage_serviceDoesNotCrash() throws Exception {
        rawProducer.send(new ProducerRecord<>("link.raw-updates", "bad", "{ this is not valid json !!"));
        rawProducer.flush();

        RawLinkUpdate valid =
                new RawLinkUpdate(55L, "This is a completely valid update message for the test", "bob", List.of(300L));
        rawProducer.send(new ProducerRecord<>("link.raw-updates", "55", objectMapper.writeValueAsString(valid)));
        rawProducer.flush();

        verify(kafkaTemplate, timeout(10_000)).send(anyString(), anyString(), any(ProcessedLinkUpdate.class));
    }

    private List<ProcessedLinkUpdate> pollMessages(Duration timeout) throws Exception {
        List<ProcessedLinkUpdate> results = new ArrayList<>();
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (results.isEmpty() && System.currentTimeMillis() < deadline) {
            for (ConsumerRecord<String, String> record : processedConsumer.poll(Duration.ofMillis(500))) {
                results.add(objectMapper.readValue(record.value(), ProcessedLinkUpdate.class));
            }
        }
        return results;
    }
}
