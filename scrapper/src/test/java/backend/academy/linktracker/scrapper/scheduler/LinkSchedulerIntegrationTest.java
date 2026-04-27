package backend.academy.linktracker.scrapper.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkUpdateService;
import backend.academy.linktracker.scrapper.service.MessageSender;
import backend.academy.linktracker.scrapper.service.UpdateInfo;
import backend.academy.linktracker.scrapper.service.UpdateType;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkSchedulerIntegrationTest {

    @Mock
    LinkRepository linkRepository;

    @Mock
    LinkUpdateService linkUpdateService;

    @Mock
    MessageSender messageSender;

    SchedulerProperties properties;

    LinkScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new SchedulerProperties();
        properties.setBatchSize(100);
        scheduler = new LinkScheduler(linkRepository, linkUpdateService, messageSender, properties);
    }

    @Test
    void batchProcessing_processesAllLinks() {
        Link link1 = new Link(1L, URI.create("https://github.com/user/repo1"), List.of(), List.of());
        Link link2 = new Link(2L, URI.create("https://github.com/user/repo2"), List.of(), List.of());
        Link link3 = new Link(3L, URI.create("https://github.com/user/repo3"), List.of(), List.of());

        Map<Link, List<Long>> batch = new LinkedHashMap<>();
        batch.put(link1, List.of(1L));
        batch.put(link2, List.of(2L));
        batch.put(link3, List.of(3L));

        when(linkRepository.findLinksToCheck(100)).thenReturn(batch);
        when(linkUpdateService.checkUpdate(any())).thenReturn(Optional.empty());

        scheduler.checkUpdates();

        verify(linkUpdateService, times(3)).checkUpdate(any(Link.class));
    }

    @Test
    void errorInOneLink_doesNotStopOthers() {
        Link link1 = new Link(1L, URI.create("https://github.com/user/repo1"), List.of(), List.of());
        Link link2 = new Link(2L, URI.create("https://github.com/user/repo2"), List.of(), List.of());
        Link link3 = new Link(3L, URI.create("https://github.com/user/repo3"), List.of(), List.of());

        Map<Link, List<Long>> batch = new LinkedHashMap<>();
        batch.put(link1, List.of(1L));
        batch.put(link2, List.of(2L));
        batch.put(link3, List.of(3L));

        when(linkRepository.findLinksToCheck(100)).thenReturn(batch);
        when(linkUpdateService.checkUpdate(link1)).thenReturn(Optional.empty());
        when(linkUpdateService.checkUpdate(link2)).thenThrow(new RuntimeException("API Error"));
        when(linkUpdateService.checkUpdate(link3)).thenReturn(Optional.empty());

        scheduler.checkUpdates();

        verify(linkUpdateService).checkUpdate(link1);
        verify(linkUpdateService).checkUpdate(link2);
        verify(linkUpdateService).checkUpdate(link3);

        verify(messageSender).sendError(eq(link2), any(), eq(List.of(2L)));
    }

    @Test
    void updateFound_sendsNotification() {
        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());

        UpdateInfo updateInfo = new UpdateInfo(
                "user/repo",
                Map.of(
                        UpdateType.GITHUB_ISSUE,
                        List.of(new UpdateInfo.UpdateItem("New Issue", "author", Instant.now(), "preview"))));

        when(linkRepository.findLinksToCheck(100)).thenReturn(Map.of(link, List.of(1L, 2L)));
        when(linkUpdateService.checkUpdate(link)).thenReturn(Optional.of(updateInfo));

        scheduler.checkUpdates();

        verify(messageSender).sendUpdate(eq(link), eq(updateInfo), eq(List.of(1L, 2L)));
        verify(linkRepository).updateLastCheckedAt(eq(1L), any(Instant.class));
    }

    @Test
    void noUpdates_doesNotSendNotification() {
        Link link = new Link(1L, URI.create("https://github.com/user/repo"), List.of(), List.of());

        when(linkRepository.findLinksToCheck(100)).thenReturn(Map.of(link, List.of(1L)));
        when(linkUpdateService.checkUpdate(link)).thenReturn(Optional.empty());

        scheduler.checkUpdates();

        verify(messageSender, never()).sendUpdate(any(), any(), any());
    }

    @Test
    void emptyBatch_doesNothing() {
        when(linkRepository.findLinksToCheck(100)).thenReturn(Map.of());

        scheduler.checkUpdates();

        verify(linkUpdateService, never()).checkUpdate(any());
        verify(messageSender, never()).sendUpdate(any(), any(), any());
    }

    @Test
    void respectsBatchSize() {
        when(linkRepository.findLinksToCheck(100)).thenReturn(Map.of());

        scheduler.checkUpdates();

        verify(linkRepository).findLinksToCheck(100);
    }
}
