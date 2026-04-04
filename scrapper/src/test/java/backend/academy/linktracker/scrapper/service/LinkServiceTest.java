package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.InMemory.InMemoryChatRepository;
import backend.academy.linktracker.scrapper.repository.InMemory.InMemoryLinkRepository;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkServiceTest {

    LinkService linkService;
    ChatService chatService;

    private static final long CHAT_ID = 1L;
    private static final URI URL = URI.create("https://github.com/user/repo");

    @BeforeEach
    void setUp() {
        var chatRepository = new InMemoryChatRepository();
        var linkRepository = new InMemoryLinkRepository();
        chatService = new ChatService(chatRepository);
        linkService = new LinkService(linkRepository, chatRepository);
        chatService.register(CHAT_ID);
    }

    @Test
    void add_newLink_success() {
        Link link = linkService.add(CHAT_ID, URL, List.of("work"), List.of());
        assertEquals(URL, link.getUrl());
        assertEquals(List.of("work"), link.getTags());
    }

    @Test
    void add_nonExistentChat_throwsException() {
        assertThrows(ChatNotFoundException.class, () -> linkService.add(999L, URL, List.of(), List.of()));
    }

    @Test
    void add_duplicateLink_throwsException() {
        linkService.add(CHAT_ID, URL, List.of(), List.of());
        assertThrows(LinkAlreadyExistsException.class, () -> linkService.add(CHAT_ID, URL, List.of(), List.of()));
    }

    @Test
    void remove_existingLink_success() {
        linkService.add(CHAT_ID, URL, List.of(), List.of());
        linkService.remove(CHAT_ID, URL);
        assertTrue(linkService.findAll(CHAT_ID).isEmpty());
    }

    @Test
    void remove_nonExistentLink_throwsException() {
        assertThrows(LinkNotFoundException.class, () -> linkService.remove(CHAT_ID, URL));
    }

    @Test
    void remove_nonExistentChat_throwsException() {
        assertThrows(ChatNotFoundException.class, () -> linkService.remove(999L, URL));
    }

    @Test
    void findAll_returnsAllLinks() {
        URI url2 = URI.create("https://github.com/user/repo2");
        linkService.add(CHAT_ID, URL, List.of(), List.of());
        linkService.add(CHAT_ID, url2, List.of(), List.of());
        assertEquals(2, linkService.findAll(CHAT_ID).size());
    }

    @Test
    void findAll_nonExistentChat_throwsException() {
        assertThrows(ChatNotFoundException.class, () -> linkService.findAll(999L));
    }
}
