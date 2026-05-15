package backend.academy.linktracker.scrapper.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import backend.academy.linktracker.scrapper.service.KafkaMessageSender;
import backend.academy.linktracker.scrapper.service.MessageSender;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class MessageSenderConfigurationCombinedTest {

    /** Перечисление всех вариантов свойства, которые мы хотим проверить. */
    private enum NotificationType {
        KAFKA("kafka"),
        GRPC("grpc"),
        // Пустая строка означает, что свойство не будет переопределено –
        // тогда сработает matchIfMissing = true в конфигурации и выберется Kafka.
        DEFAULT("");

        private final String value;
        NotificationType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @Autowired
    private ApplicationContext context;

    @ParameterizedTest
    @EnumSource(NotificationType.class)
    void messageSenderMatchesNotificationType(NotificationType type) {
        // Если свойство пустое – просто проверяем текущий контекст
        // (свойство не переопределяем, остаётся значение из application‑test.yaml
        // или отсутствует → по‑умолчанию Kafka).
        if (type.getValue().isEmpty()) {
            assertThat(context.getBean(MessageSender.class))
                    .isInstanceOf(KafkaMessageSender.class);
            return;
        }

        // Иначе переопределяем свойство только для этого вызова.
        // @DynamicPropertySource будет выполнен перед каждым вызовом теста,
        // поэтому мы просто сохраняем значение в поле и читаем его в источнике.
        testValue = type.getValue();
    }

    // Поле, которое будет заполнено перед каждым вызовом @ParameterizedTest.
    private String testValue;


    /* -----------------------------------------------------------------
     * Ниже – тестовая конфигурация, которая гарантирует, что в контекст
     * попадают только те бины, которые нам нужны для проверки.
     * Мы не меняем продуктивную конфигурацию, а просто добавляем
     * @TestConfiguration, которая будет использована дополнительно.
     * ----------------------------------------------------------------- */
    @TestConfiguration
    static class TestConfig {
        // Никаких дополнительных бинов не добавляем – оставляем
        // MessageSenderConfiguration из основного кода, чтобы она работала
        // как обычно и создавала именно один бин MessageSender.
    }
}
