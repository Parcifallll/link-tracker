package backend.academy.linktracker.scrapper.repository; // Test that ORM access type uses ORM implementation

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import backend.academy.linktracker.scrapper.repository.orm.OrmChatRepository;
import backend.academy.linktracker.scrapper.repository.orm.OrmLinkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "app.database.access-type=ORM")
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OrmAccessTypeTest extends AbstractIntegrationTest {

    @Autowired
    ApplicationContext context;

    @Test
    void ormAccessType_usesOrmImplementation() {
        assertThat(context.getBean(ChatRepository.class)).isInstanceOf(OrmChatRepository.class);
        assertThat(context.getBean(LinkRepository.class)).isInstanceOf(OrmLinkRepository.class);
    }
}
