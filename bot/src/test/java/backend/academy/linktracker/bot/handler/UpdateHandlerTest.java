package backend.academy.linktracker.bot.handler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.command.CancelCommand;
import backend.academy.linktracker.bot.command.HelpCommand;
import backend.academy.linktracker.bot.command.ListCommand;
import backend.academy.linktracker.bot.command.StartCommand;
import backend.academy.linktracker.bot.command.TrackCommand;
import backend.academy.linktracker.bot.command.UnknownCommand;
import backend.academy.linktracker.bot.command.UntrackCommand;
import backend.academy.linktracker.bot.grpc.ScrapperGrpcClient;
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

    @Mock
    ScrapperGrpcClient scrapperGrpcClient;

    UpdateHandler handler;

    private static final long CHAT_ID = 123L;

    @BeforeEach
    void setUp() {
        handler = new UpdateHandler(
                List.of(
                        new StartCommand(userService, scrapperGrpcClient),
                        new HelpCommand(List.of(new StartCommand(userService, scrapperGrpcClient))),
                        new TrackCommand(userService, scrapperGrpcClient),
                        new UntrackCommand(userService, scrapperGrpcClient),
                        new ListCommand(scrapperGrpcClient),
                        new CancelCommand(userService)),
                new UnknownCommand(),
                userService);
    }

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
    void startCommand_returnsWelcomeMessage() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.IDLE);
        SendMessage response = handler.handle(buildUpdate("/start", CHAT_ID));
        assertNotNull(response);
        assertTrue(response.getParameters().get("text").toString().contains("Welcome"));
    }

    @Test
    void helpCommand_returnsCommandList() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.IDLE);
        SendMessage response = handler.handle(buildUpdate("/help", CHAT_ID));
        assertNotNull(response);
        assertTrue(response.getParameters().get("text").toString().contains("/start"));
    }

    @Test
    void unknownCommand_returnsErrorMessage() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.IDLE);
        SendMessage response = handler.handle(buildUpdate("/unknown", CHAT_ID));
        assertNotNull(response);
        assertTrue(response.getParameters().get("text").toString().contains("Unknown command"));
    }

    // TZ: пользователь отправляет другую команду во время диалога -> сессия сбрасывается
    @Test
    void newCommandDuringDialog_resetsSession() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.WAITING_LINK);
        handler.handle(buildUpdate("/help", CHAT_ID));
        verify(userService).resetSession(CHAT_ID);
    }

    // /cancel -> сбрасывает сессию
    @Test
    void cancelCommand_resetsSession() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.WAITING_LINK);
        handler.handle(buildUpdate("/cancel", CHAT_ID));
        verify(userService).resetSession(CHAT_ID);
    }

    // null message -> возвращает null без исключения
    @Test
    void nullMessage_returnsNull() {
        var update = mock(Update.class);
        when(update.message()).thenReturn(null);
        SendMessage response = handler.handle(update);
        assertTrue(response == null);
    }

    // requiresRegistration: незарегистрированный пользователь -> просит /start
    @Test
    void unregisteredUser_commandRequiresRegistration_asksToStart() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.IDLE);
        when(userService.isNewUser(CHAT_ID)).thenReturn(true);
        SendMessage response = handler.handle(buildUpdate("/list", CHAT_ID));
        assertNotNull(response);
        assertTrue(response.getParameters().get("text").toString().contains("/start"));
    }
}
