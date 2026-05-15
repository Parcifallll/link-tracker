package backend.academy.linktracker.bot.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaUpdateListenerTest {

    @Mock
    private TelegramBot telegramBot;

    private KafkaUpdateListener kafkaUpdateListener;

    @BeforeEach
    void setUp() {
        kafkaUpdateListener = new KafkaUpdateListener(telegramBot);
    }

    @Test
    void handleUpdate_withMultipleChatIds_sendMessageToEachChat() {
        // Arrange
        LinkUpdate update = new LinkUpdate(
            1L,
            URI.create("https://github.com/user/repo"),
            "New Release",
            List.of(123L, 456L, 789L)
        );

        // Act
        kafkaUpdateListener.handleUpdate(update);

        // Assert
        verify(telegramBot, times(3)).execute(any(SendMessage.class));
    }

    @Test
    void handleUpdate_withSingleChatId_sendMessageOnce() {
        // Arrange
        LinkUpdate update = new LinkUpdate(
            1L,
            URI.create("https://github.com/user/repo"),
            "New Release",
            List.of(123L)
        );

        // Act
        kafkaUpdateListener.handleUpdate(update);

        // Assert
        verify(telegramBot, times(1)).execute(any(SendMessage.class));
    }

    @Test
    void handleUpdate_withEmptyChatIds_noMessageSent() {
        // Arrange
        LinkUpdate update = new LinkUpdate(
            1L,
            URI.create("https://github.com/user/repo"),
            "New Release",
            List.of()
        );

        // Act
        kafkaUpdateListener.handleUpdate(update);

        // Assert
        verify(telegramBot, times(0)).execute(any(SendMessage.class));
    }

    @Test
    void handleUpdate_withError_stillProcesses() {
        // Arrange
        LinkUpdate update = new LinkUpdate(
            1L,
            URI.create("https://github.com/user/repo"),
            "Error: Connection timeout",
            List.of(123L, 456L)
        );

        // Act
        kafkaUpdateListener.handleUpdate(update);

        // Assert
        verify(telegramBot, times(2)).execute(any(SendMessage.class));
    }
}
