package backend.academy.linktracker.bot;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.command.HelpCommand;
import backend.academy.linktracker.bot.command.StartCommand;
import backend.academy.linktracker.bot.command.UnknownCommand;
import backend.academy.linktracker.bot.handler.UpdateHandler;
import backend.academy.linktracker.bot.model.UserState;
import backend.academy.linktracker.bot.service.UserService;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateHandlerTest {

    @Mock
    UserService userService;

    UpdateHandler handler;

    @BeforeEach
    void setUp() {
        var startCommand = new StartCommand(userService);
        var unknownCommand = new UnknownCommand();

        var helpCommand = new HelpCommand(List.of(startCommand));
        when(userService.getState(anyLong())).thenReturn(UserState.IDLE);
        handler = new UpdateHandler(List.of(startCommand, helpCommand), unknownCommand, userService);
    }

    // Helper to build a fake Update with given text and chatId
    private Update buildUpdate(String text, long chatId) {
        var chat = mock(Chat.class);
        when(chat.id()).thenReturn(chatId);

        var message = mock(Message.class);
        when(message.text()).thenReturn(text);
        when(message.chat()).thenReturn(chat);

        var update = mock(Update.class);
        when(update.message()).thenReturn(message);

        return update;
    }

    @Test
    void startCommand_shouldReturnWelcomeMessage() {
        var update = buildUpdate("/start", 123L);

        SendMessage response = handler.handle(update);

        assertNotNull(response);
        assertTrue(response.getParameters().get("text").toString().contains("Welcome!"));
    }

    @Test
    void helpCommand_shouldReturnCommandList() {
        var update = buildUpdate("/help", 123L);

        SendMessage response = handler.handle(update);

        assertNotNull(response);
        String text = response.getParameters().get("text").toString();
        assertTrue(text.contains("/start"));
    }

    @Test
    void unknownCommand_shouldReturnErrorMessage() {
        var update = buildUpdate("/unknown", 123L);

        SendMessage response = handler.handle(update);

        assertNotNull(response);
        assertTrue(response.getParameters().get("text").toString().contains("Unknown command"));
    }
}
