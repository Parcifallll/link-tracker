package backend.academy.linktracker.bot.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.grpc.SendUpdateRequest;
import backend.academy.linktracker.grpc.SendUpdateResponse;
import backend.academy.linktracker.grpc.UpdateItem;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BotUpdateGrpcServiceTest {

    static class CapturingBot extends TelegramBot {

        final List<SendMessage> sent = new ArrayList<>();

        CapturingBot() {
            super("test-token");
        }

        @Override
        public <T extends BaseRequest<T, R>, R extends BaseResponse> R execute(BaseRequest<T, R> request) {
            if (request instanceof SendMessage msg) {
                sent.add(msg);
            }
            return null;
        }
    }

    static class CapturingObserver implements StreamObserver<SendUpdateResponse> {

        SendUpdateResponse response;
        Throwable error;
        boolean completed;

        @Override
        public void onNext(SendUpdateResponse value) {
            this.response = value;
        }

        @Override
        public void onError(Throwable t) {
            this.error = t;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }
    }

    CapturingBot bot;
    BotUpdateGrpcService service;
    CapturingObserver observer;

    @BeforeEach
    void setUp() {
        bot = new CapturingBot();
        service = new BotUpdateGrpcService(bot);
        observer = new CapturingObserver();
    }

    private SendUpdateRequest buildUpdate(List<Long> chatIds, String url, String title) {
        return SendUpdateRequest.newBuilder()
                .setUrl(url)
                .setTitle(title)
                .setError("")
                .addAllTgChatIds(chatIds)
                .addUpdates(UpdateItem.newBuilder()
                        .setType("GITHUB_ISSUE")
                        .setTitle("Fix bug")
                        .setAuthor("alice")
                        .setCreatedAt("2026-01-01T00:00:00Z")
                        .setPreview("important fix")
                        .build())
                .build();
    }

    private SendUpdateRequest buildError(List<Long> chatIds, String url, String error) {
        return SendUpdateRequest.newBuilder()
                .setUrl(url)
                .setError(error)
                .addAllTgChatIds(chatIds)
                .build();
    }

    @Test
    void singleChat_updateSent_onNextCalledWithSuccess() {
        service.sendUpdate(buildUpdate(List.of(100L), "https://github.com/user/repo", "user/repo"), observer);

        assertThat(observer.response).isNotNull();
        assertThat(observer.response.getSuccess()).isTrue();
        assertThat(observer.error).isNull();
        assertThat(observer.completed).isTrue();
    }

    @Test
    void singleChat_telegramBotReceivesExactlyOneMessage() {
        service.sendUpdate(buildUpdate(List.of(100L), "https://github.com/user/repo", "user/repo"), observer);

        assertThat(bot.sent).hasSize(1);
    }

    @Test
    void multipleChats_telegramBotReceivesOneMessagePerChat() {
        List<Long> chatIds = List.of(1L, 2L, 3L);
        service.sendUpdate(buildUpdate(chatIds, "https://github.com/user/repo", "user/repo"), observer);

        assertThat(bot.sent).hasSize(3);
    }

    @Test
    void multipleChats_eachMessageSentToCorrectChatId() {
        List<Long> chatIds = List.of(10L, 20L);
        service.sendUpdate(buildUpdate(chatIds, "https://github.com/user/repo", "user/repo"), observer);

        List<Object> actualIds =
                bot.sent.stream().map(msg -> msg.getParameters().get("chat_id")).toList();

        assertThat(actualIds).containsExactlyInAnyOrder(10L, 20L);
    }

    @Test
    void errorRequest_messageContainsErrorText() {
        service.sendUpdate(buildError(List.of(42L), "https://github.com/user/repo", "timeout"), observer);

        assertThat(bot.sent).hasSize(1);
        String text = (String) bot.sent.getFirst().getParameters().get("text");
        assertThat(text).contains("timeout");
        assertThat(text).contains("https://github.com/user/repo");
    }

    @Test
    void normalUpdate_messageContainsUrlAndTitle() {
        service.sendUpdate(buildUpdate(List.of(1L), "https://github.com/user/repo", "user/repo"), observer);

        String text = (String) bot.sent.getFirst().getParameters().get("text");
        assertThat(text).contains("https://github.com/user/repo");
        assertThat(text).contains("user/repo");
    }

    @Test
    void normalUpdate_messageContainsUpdateItemDetails() {
        service.sendUpdate(buildUpdate(List.of(1L), "https://github.com/user/repo", "user/repo"), observer);

        String text = (String) bot.sent.getFirst().getParameters().get("text");
        assertThat(text).contains("Fix bug");
        assertThat(text).contains("alice");
        assertThat(text).contains("important fix");
    }

    @Test
    void emptyUpdatesList_stillCompletesSuccessfully() {
        SendUpdateRequest request = SendUpdateRequest.newBuilder()
                .setUrl("https://github.com/user/repo")
                .setTitle("user/repo")
                .setError("")
                .addTgChatIds(1L)
                .build();

        service.sendUpdate(request, observer);

        assertThat(observer.response.getSuccess()).isTrue();
        assertThat(bot.sent).hasSize(1);
    }

    @Test
    void noChatIds_noMessagesDelivered_stillCompletesSuccessfully() {
        SendUpdateRequest request = SendUpdateRequest.newBuilder()
                .setUrl("https://github.com/user/repo")
                .setError("")
                .build();

        service.sendUpdate(request, observer);

        assertThat(bot.sent).isEmpty();
        assertThat(observer.completed).isTrue();
    }
}
