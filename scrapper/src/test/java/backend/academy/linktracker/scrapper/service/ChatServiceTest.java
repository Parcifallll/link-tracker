package backend.academy.linktracker.scrapper.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import backend.academy.linktracker.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.repository.InMemoryChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

    ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(new InMemoryChatRepository());
    }

    @Test
    void register_newChat_success() {
        chatService.register(1L);
        assertTrue(chatService.exists(1L));
    }

    @Test
    void register_existingChat_throwsException() {
        chatService.register(1L);
        assertThrows(ChatAlreadyExistsException.class, () -> chatService.register(1L));
    }

    @Test
    void delete_existingChat_success() {
        chatService.register(1L);
        chatService.delete(1L);
        assertFalse(chatService.exists(1L));
    }

    @Test
    void delete_nonExistentChat_throwsException() {
        assertThrows(ChatNotFoundException.class, () -> chatService.delete(999L));
    }
}
