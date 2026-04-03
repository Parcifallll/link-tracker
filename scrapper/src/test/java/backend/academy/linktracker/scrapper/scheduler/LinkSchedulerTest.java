package backend.academy.linktracker.scrapper.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.grpc.BotUpdateGrpcClient;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkUpdateService;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkSchedulerTest {

    @Mock
    LinkRepository linkRepository;

    @Mock
    LinkUpdateService linkUpdateService;

    @Mock
    BotUpdateGrpcClient botUpdateGrpcClient;

    LinkScheduler linkScheduler;

    @BeforeEach
    void setUp() {
        linkScheduler = new LinkScheduler(linkRepository, linkUpdateService, botUpdateGrpcClient);
    }

    private Link buildLink(String url) {
        return new Link(1L, URI.create(url), List.of(), List.of());
    }

    @Test
    void hasUpdate_sendsNotification() {
        Link link = buildLink("https://github.com/user/repo");
        when(linkRepository.findAllWithChatIds()).thenReturn(Map.of(1L, List.of(link)));
        when(linkUpdateService.hasUpdate(link)).thenReturn(true);

        linkScheduler.checkUpdates();

        verify(botUpdateGrpcClient).sendUpdate(eq(link), eq(List.of(1L)));
    }

    @Test
    void noUpdate_doesNotSendNotification() {
        Link link = buildLink("https://github.com/user/repo");
        when(linkRepository.findAllWithChatIds()).thenReturn(Map.of(1L, List.of(link)));
        when(linkUpdateService.hasUpdate(link)).thenReturn(false);

        linkScheduler.checkUpdates();

        verify(botUpdateGrpcClient, never()).sendUpdate(any(), any());
    }

    @Test
    void emptyRepository_doesNotSendNotification() {
        when(linkRepository.findAllWithChatIds()).thenReturn(Map.of());

        linkScheduler.checkUpdates();

        verify(botUpdateGrpcClient, never()).sendUpdate(any(), any());
    }

    @Test
    void onlySubscribedChatReceivesNotification() {
        Link link1 = buildLink("https://github.com/user/repo1");
        Link link2 = buildLink("https://github.com/user/repo2");
        when(linkRepository.findAllWithChatIds())
                .thenReturn(Map.of(
                        1L, List.of(link1),
                        2L, List.of(link2)));
        when(linkUpdateService.hasUpdate(link1)).thenReturn(true);
        when(linkUpdateService.hasUpdate(link2)).thenReturn(false);

        linkScheduler.checkUpdates();

        verify(botUpdateGrpcClient).sendUpdate(eq(link1), eq(List.of(1L)));
        verify(botUpdateGrpcClient, never()).sendUpdate(eq(link2), any());
    }
}
