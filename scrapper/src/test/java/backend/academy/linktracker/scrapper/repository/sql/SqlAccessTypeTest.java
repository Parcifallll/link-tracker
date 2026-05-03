package backend.academy.linktracker.scrapper.repository.sql; // Test that SQL access type uses SQL implementation

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "app.database.access-type=SQL")
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SqlAccessTypeTest extends AbstractIntegrationTest {

    @Autowired
    ApplicationContext context;

    @Test
    void sqlAccessType_usesSqlImplementation() {
        assertThat(context.getBean(ChatRepository.class)).isInstanceOf(SqlChatRepository.class);
        assertThat(context.getBean(LinkRepository.class)).isInstanceOf(SqlLinkRepository.class);
    }
}
