package backend.academy.linktracker.scrapper.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import backend.academy.linktracker.scrapper.model.Chat;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkUpdateService;
import backend.academy.linktracker.scrapper.service.MessageSender;
import backend.academy.linktracker.scrapper.service.UpdateInfo;
import backend.academy.linktracker.scrapper.service.UpdateType;
import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {"app.database.access-type=ORM", "app.scheduler.interval=PT1H"})
@Testcontainers
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class LinkSchedulerIntegrationTest {

    @Autowired
    LinkScheduler scheduler;

    @Autowired
    LinkRepository linkRepository;

    @Autowired
    ChatRepository chatRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    SchedulerProperties schedulerProperties;

    @MockitoBean
    LinkUpdateService linkUpdateService;

    @MockitoBean
    MessageSender messageSender;

    private static final long CHAT_ID = 1L;

    private static final UpdateInfo FAKE_UPDATE = new UpdateInfo(
            "user/repo",
            Map.of(
                    UpdateType.GITHUB_ISSUE,
                    List.of(new UpdateInfo.UpdateItem("Issue title", "author", Instant.now(), "preview"))));

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM subscriptions");
        jdbcTemplate.update("DELETE FROM links");
        jdbcTemplate.update("DELETE FROM chats");

        chatRepository.save(new Chat(CHAT_ID));
        schedulerProperties.setBatchSize(100);
    }

    @Test
    void emptyDatabase_checkUpdates_doesNothing() {
        scheduler.checkUpdates();

        verify(linkUpdateService, never()).checkUpdate(any());
        verify(messageSender, never()).sendUpdate(any(), any(), any());
    }

    @Test
    void linksPresent_updatesFound_sendsNotification() {
        Link link = new Link(0, URI.create("https://github.com/user/repo"), List.of(), List.of());
        linkRepository.save(CHAT_ID, link);

        when(linkUpdateService.checkUpdate(any())).thenReturn(Optional.of(FAKE_UPDATE));

        scheduler.checkUpdates();

        verify(messageSender).sendUpdate(any(Link.class), eq(FAKE_UPDATE), any());

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT last_checked_at FROM links WHERE url = ?", "https://github.com/user/repo");

        assertThat(rows).isNotEmpty();
        assertThat(rows.get(0).get("last_checked_at")).isNotNull();
    }

    @Test
    void linksPresent_noUpdatesFound_doesNotSend() {
        linkRepository.save(CHAT_ID, new Link(0, URI.create("https://github.com/user/repo"), List.of(), List.of()));
        when(linkUpdateService.checkUpdate(any())).thenReturn(Optional.empty());

        scheduler.checkUpdates();

        verify(messageSender, never()).sendUpdate(any(), any(), any());
    }

    @Test
    void errorInOneLink_doesNotStopProcessingOthers() {
        URI url1 = URI.create("https://github.com/user/repo1");
        URI url2 = URI.create("https://github.com/user/repo2");
        URI url3 = URI.create("https://github.com/user/repo3");

        linkRepository.save(CHAT_ID, new Link(0, url1, List.of(), List.of()));
        linkRepository.save(CHAT_ID, new Link(0, url2, List.of(), List.of()));
        linkRepository.save(CHAT_ID, new Link(0, url3, List.of(), List.of()));

        // Spread timestamps so processing order is deterministic (oldest first)
        jdbcTemplate.update(
                "UPDATE links SET last_checked_at = ? WHERE url = ?",
                Timestamp.from(Instant.EPOCH.plusSeconds(1)),
                url1.toString());
        jdbcTemplate.update(
                "UPDATE links SET last_checked_at = ? WHERE url = ?",
                Timestamp.from(Instant.EPOCH.plusSeconds(2)),
                url2.toString());
        jdbcTemplate.update(
                "UPDATE links SET last_checked_at = ? WHERE url = ?",
                Timestamp.from(Instant.EPOCH.plusSeconds(3)),
                url3.toString());

        when(linkUpdateService.checkUpdate(
                        argThat(link -> link != null && link.getUrl().equals(url1))))
                .thenReturn(Optional.empty());

        when(linkUpdateService.checkUpdate(
                        argThat(link -> link != null && link.getUrl().equals(url2))))
                .thenThrow(new RuntimeException("API Error"));

        when(linkUpdateService.checkUpdate(
                        argThat(link -> link != null && link.getUrl().equals(url3))))
                .thenReturn(Optional.empty());

        scheduler.checkUpdates();

        // All three links must have been attempted
        verify(linkUpdateService, times(3)).checkUpdate(any(Link.class));

        verify(messageSender)
                .sendError(argThat(link -> link != null && link.getUrl().equals(url2)), any(), any());

        verify(messageSender, never())
                .sendError(argThat(link -> link != null && !link.getUrl().equals(url2)), any(), any());
    }

    @Test
    void chatIdsCorrectlyPassedToMessageSender() {
        long chatId2 = 2L;
        chatRepository.save(new Chat(chatId2));

        URI url = URI.create("https://github.com/user/shared-repo");
        linkRepository.save(CHAT_ID, new Link(0, url, List.of(), List.of()));
        linkRepository.save(chatId2, new Link(0, url, List.of(), List.of()));

        when(linkUpdateService.checkUpdate(any())).thenReturn(Optional.of(FAKE_UPDATE));

        scheduler.checkUpdates();

        verify(messageSender)
                .sendUpdate(any(), eq(FAKE_UPDATE), argThat(ids -> ids.containsAll(List.of(CHAT_ID, chatId2))));
    }

    @Test
    void respectsBatchSize_onlyFetchesUpToLimit() {
        for (int i = 0; i < 10; i++) {
            linkRepository.save(
                    CHAT_ID, new Link(0, URI.create("https://github.com/user/repo" + i), List.of(), List.of()));
        }
        schedulerProperties.setBatchSize(3);
        when(linkUpdateService.checkUpdate(any())).thenReturn(Optional.empty());

        scheduler.checkUpdates();

        verify(linkUpdateService, times(3)).checkUpdate(any(Link.class));
    }

    @Test
    void tenLinks_batchSizeFive_allRecordsUpdatedInTwoRuns() {
        for (int i = 0; i < 10; i++) {
            URI url = URI.create("https://github.com/user/repo" + i);
            linkRepository.save(CHAT_ID, new Link(0, url, List.of(), List.of()));
            jdbcTemplate.update(
                    "UPDATE links SET last_checked_at = ? WHERE url = ?",
                    Timestamp.from(Instant.EPOCH.plusSeconds(i)),
                    url.toString());
        }

        schedulerProperties.setBatchSize(5);

        doAnswer(inv -> {
                    Link link = inv.getArgument(0);
                    link.setLastCheckedAt(Instant.now());
                    return Optional.of(FAKE_UPDATE);
                })
                .when(linkUpdateService)
                .checkUpdate(any(Link.class));

        scheduler.checkUpdates();
        verify(messageSender, times(5)).sendUpdate(any(), any(), any());

        scheduler.checkUpdates();
        verify(messageSender, times(10)).sendUpdate(any(), any(), any());
    }
}
