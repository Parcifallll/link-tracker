package backend.academy.linktracker.bot.command;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.grpc.ScrapperGrpcClient;
import backend.academy.linktracker.bot.model.UserState;
import backend.academy.linktracker.bot.service.UserService;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrackCommandTest {

    @Mock
    UserService userService;

    @Mock
    ScrapperGrpcClient scrapperGrpcClient;

    TrackCommand trackCommand;

    private static final long CHAT_ID = 123L;

    @BeforeEach
    void setUp() {
        trackCommand = new TrackCommand(userService, scrapperGrpcClient);
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

    // /track -> запускает диалог, просит ссылку
    @Test
    void idle_startsDialog() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.IDLE);
        SendMessage response = trackCommand.handle(buildUpdate("/track"));
        assertNotNull(response);
        verify(userService).setState(CHAT_ID, UserState.WAITING_LINK);
    }

    // TZ: некорректная ссылка -> бот уведомляет об ошибке
    @Test
    void waitingLink_invalidUrl_returnsError() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.WAITING_LINK);
        SendMessage response = trackCommand.handle(buildUpdate("tbank://not-valid"));
        assertNotNull(response);
        assertTrue(response.getParameters().get("text").toString().toLowerCase().contains("invalid"));
        verify(userService, never()).setState(CHAT_ID, UserState.WAITING_TAGS);
    }

    // TZ: ссылка уже отслеживается -> уведомляет сразу, не спрашивает теги
    @Test
    void waitingLink_alreadyTracked_notifiesImmediately() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.WAITING_LINK);
        when(scrapperGrpcClient.linkExists(CHAT_ID, "https://github.com/user/repo"))
                .thenReturn(true);
        SendMessage response = trackCommand.handle(buildUpdate("https://github.com/user/repo"));
        assertNotNull(response);
        String text = response.getParameters().get("text").toString().toLowerCase();
        assertTrue(text.contains("уже") || text.contains("already"));
        verify(userService, never()).setState(CHAT_ID, UserState.WAITING_TAGS);
    }

    // TZ: корректная ссылка -> сохраняется, переходим к тегам
    @Test
    void waitingLink_validUrl_proceedsToTags() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.WAITING_LINK);
        when(scrapperGrpcClient.linkExists(CHAT_ID, "https://github.com/user/repo"))
                .thenReturn(false);
        SendMessage response = trackCommand.handle(buildUpdate("https://github.com/user/repo"));
        assertNotNull(response);
        verify(userService).setPendingLink(CHAT_ID, URI.create("https://github.com/user/repo"));
        verify(userService).setState(CHAT_ID, UserState.WAITING_TAGS);
    }

    // теги -> сохраняются, переходим к фильтрам
    @Test
    void waitingTags_savesTags_proceedsToFilters() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.WAITING_TAGS);
        SendMessage response = trackCommand.handle(buildUpdate("work, java"));
        assertNotNull(response);
        verify(userService).setTags(CHAT_ID, List.of("work", "java"));
        verify(userService).setState(CHAT_ID, UserState.WAITING_FILTERS);
    }

    // skip тегов -> пустой список
    @Test
    void waitingTags_skip_savesEmptyList() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.WAITING_TAGS);
        trackCommand.handle(buildUpdate("skip"));
        verify(userService).setTags(CHAT_ID, List.of());
    }

    // TZ: полный флоу -> ссылка трекается
    @Test
    void waitingFilters_tracksLink() {
        when(userService.getState(CHAT_ID)).thenReturn(UserState.WAITING_FILTERS);
        when(userService.getPendingLink(CHAT_ID)).thenReturn(URI.create("https://github.com/user/repo"));
        when(userService.getTags(CHAT_ID)).thenReturn(List.of("work"));
        SendMessage response = trackCommand.handle(buildUpdate("skip"));
        assertNotNull(response);
        verify(scrapperGrpcClient).trackLink(anyLong(), anyString(), any(), any());
        assertTrue(response.getParameters().get("text").toString().toLowerCase().contains("tracked"));
    }
}
