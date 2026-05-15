package backend.academy.linktracker.scrapper.service.senders;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class DefaultMessageSenderConfigurationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void defaultNotificationType_usesKafkaMessageSender() {
        assertThat(context.getBean(MessageSender.class)).isInstanceOf(KafkaMessageSender.class);
    }
}
