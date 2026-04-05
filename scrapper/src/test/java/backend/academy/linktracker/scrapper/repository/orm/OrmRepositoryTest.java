package backend.academy.linktracker.scrapper.repository.orm;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import backend.academy.linktracker.scrapper.model.Chat;
import backend.academy.linktracker.scrapper.model.Link;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "app.database.access-type=ORM")
@Testcontainers
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OrmRepositoryTest extends AbstractIntegrationTest {
    @Autowired
    OrmChatRepository chatRepository;

    @Autowired
    OrmLinkRepository linkRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private static final long CHAT_ID = 1L;
    private static final URI URL = URI.create("https://github.com/user/repo");

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM subscriptions");
        jdbcTemplate.update("DELETE FROM links");
        jdbcTemplate.update("DELETE FROM chats");

        chatRepository.save(new Chat(CHAT_ID));
    }

    @Test
    void addLink_savedInDatabase() {
        Link link = linkRepository.save(CHAT_ID, new Link(0, URL, List.of("tag1"), List.of()));

        assertThat(link.getId()).isPositive();
        assertThat(linkRepository.findByUrl(CHAT_ID, URL)).isPresent();
    }

    @Test
    void deleteLink_removedFromDatabase() {
        linkRepository.save(CHAT_ID, new Link(0, URL, List.of(), List.of()));
        linkRepository.delete(CHAT_ID, URL);

        assertThat(linkRepository.findByUrl(CHAT_ID, URL)).isEmpty();
    }

    @Test
    void addDuplicateLink_throwsException() {
        linkRepository.save(CHAT_ID, new Link(0, URL, List.of(), List.of()));
        Optional<Link> saved = linkRepository.findByUrl(CHAT_ID, URL);
        assertThat(saved).isPresent();

        linkRepository.save(CHAT_ID, new Link(0, URL, List.of("newTag"), List.of()));
        assertThat(linkRepository.findAll(CHAT_ID)).hasSize(1);
    }

    @Test
    void findAll_returnsAllLinks() {
        URI url2 = URI.create("https://github.com/user/repo2");
        linkRepository.save(CHAT_ID, new Link(0, URL, List.of(), List.of()));
        linkRepository.save(CHAT_ID, new Link(0, url2, List.of(), List.of()));

        assertThat(linkRepository.findAll(CHAT_ID)).hasSize(2);
    }

    @Test
    void migrationsApplied_tablesExist() {
        Integer chatsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'chats'", Integer.class);
        Integer linksCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'links'", Integer.class);
        Integer subsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'subscriptions'", Integer.class);

        assertThat(chatsCount).isEqualTo(1);
        assertThat(linksCount).isEqualTo(1);
        assertThat(subsCount).isEqualTo(1);
    }
}
