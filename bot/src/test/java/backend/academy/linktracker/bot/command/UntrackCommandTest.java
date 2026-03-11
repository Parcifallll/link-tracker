package backend.academy.linktracker.bot.command;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.grpc.ScrapperGrpcClient;
import backend.academy.linktracker.bot.model.UserState;
import backend.academy.linktracker.bot.service.UserService;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UntrackCommandTest {

    @Mock
    UserService userService;

    @Mock
    ScrapperGrpcClient scrapperGrpcClient;

    UntrackCommand untrackCommand;

    private static final long CHAT_ID = 123L;

    @BeforeEach
    void setUp() {
        untrackCommand = new UntrackCommand(userService, scrapperGrpcClient);
    }

    private Update buildUpdate(String text) {
        var chat = mock(Chat.class);
        when(chat.id()).thenReturn(CHAT_ID);
        var message = mock(Message.class);
        when(message.text()).thenReturn(text);
        when(message.chat()).thenReturn(chat);
        var update = mock(Update.class);
        when(update.message()).thenReturn(message);
        return update;
    }

    @Test
    void idle_startsDialog() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.IDLE);
        SendMessage response = untrackCommand.handle(buildUpdate("/untrack"));
        assertNotNull(response);
        verify(userService).setState(CHAT_ID, UserState.WAITING_UNTRACK_LINK);
    }

    @Test
    void waitingLink_invalidUrl_returnsError() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.WAITING_UNTRACK_LINK);
        SendMessage response = untrackCommand.handle(buildUpdate("tbank://not-valid"));
        assertNotNull(response);
        assertTrue(response.getParameters().get("text").toString().toLowerCase().contains("invalid"));
    }

    @Test
    void waitingLink_validUrl_untracksLink() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.WAITING_UNTRACK_LINK);
        SendMessage response = untrackCommand.handle(buildUpdate("https://github.com/user/repo"));
        assertNotNull(response);
        verify(scrapperGrpcClient).untrackLink(CHAT_ID, "https://github.com/user/repo");
        verify(userService).resetSession(CHAT_ID);
        assertTrue(response.getParameters().get("text").toString().toLowerCase().contains("no longer"));
    }
}
