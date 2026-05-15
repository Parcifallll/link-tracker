package backend.academy.linktracker.scrapper.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import backend.academy.linktracker.scrapper.grpc.GrpcMessageSender;
import backend.academy.linktracker.scrapper.service.MessageSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "app.notification.type=grpc")
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class GrpcMessageSenderConfigurationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void grpcNotificationType_usesGrpcMessageSender() {
        assertThat(context.getBean(MessageSender.class))
            .isInstanceOf(GrpcMessageSender.class);
    }
}
