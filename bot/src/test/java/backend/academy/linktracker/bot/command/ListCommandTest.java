package backend.academy.linktracker.bot.command;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.grpc.ScrapperGrpcClient;
import backend.academy.linktracker.grpc.LinkItem;
import backend.academy.linktracker.grpc.ListLinksResponse;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListCommandTest {

    @Mock
    ScrapperGrpcClient scrapperGrpcClient;

    ListCommand listCommand;

    private static final long CHAT_ID = 123L;

    @BeforeEach
    void setUp() {
        listCommand = new ListCommand(scrapperGrpcClient);
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

    // TZ: /list, есть подписки -> список ссылок
    @Test
    void withLinks_returnsList() {
        var linkItem = LinkItem.newBuilder()
                .setId(1L)
                .setUrl("https://github.com/user/repo")
                .build();
        when(scrapperGrpcClient.listLinks(CHAT_ID, null))
                .thenReturn(ListLinksResponse.newBuilder().addLinks(linkItem).build());

        SendMessage response = listCommand.handle(buildUpdate("/list"));
        assertNotNull(response);
        assertTrue(response.getParameters().get("text").toString().contains("https://github.com/user/repo"));
    }

    // TZ: /list, нет подписок -> сообщение "нет ссылок"
    @Test
    void noLinks_returnsEmptyMessage() {
        when(scrapperGrpcClient.listLinks(CHAT_ID, null))
                .thenReturn(ListLinksResponse.newBuilder().build());

        SendMessage response = listCommand.handle(buildUpdate("/list"));
        assertNotNull(response);
        assertTrue(response.getParameters().get("text").toString().toLowerCase().contains("no tracked"));
    }

    // TZ: /list <tag> -> передаёт тег в клиент
    @Test
    void withTagFilter_passesTagToClient() {
        when(scrapperGrpcClient.listLinks(CHAT_ID, "work"))
                .thenReturn(ListLinksResponse.newBuilder().build());

        listCommand.handle(buildUpdate("/list work"));

        verify(scrapperGrpcClient).listLinks(CHAT_ID, "work");
    }

    // TZ: /list <tag>, есть подписки с тегом -> возвращает только их
    @Test
    void withTagFilter_returnsFilteredLinks() {
        var linkItem = LinkItem.newBuilder()
                .setId(1L)
                .setUrl("https://github.com/user/repo")
                .addTags("work")
                .build();
        when(scrapperGrpcClient.listLinks(CHAT_ID, "work"))
                .thenReturn(ListLinksResponse.newBuilder().addLinks(linkItem).build());

        SendMessage response = listCommand.handle(buildUpdate("/list work"));
        assertNotNull(response);
        assertTrue(response.getParameters().get("text").toString().contains("https://github.com/user/repo"));
    }
}
