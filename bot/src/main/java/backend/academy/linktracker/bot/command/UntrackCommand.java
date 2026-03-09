package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.grpc.ScrapperGrpcClient;
import backend.academy.linktracker.bot.model.UserState;
import backend.academy.linktracker.bot.service.UserService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UntrackCommand implements Command {

    private final UserService userService;
    private final ScrapperGrpcClient scrapperGrpcClient;

    @Override
    public String command() {
        return "/untrack";
    }

    @Override
    public String description() {
        return "Stop tracking a link";
    }

    @Override
    public String message() {
        return "Send me the link you want to stop tracking";
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        UserState state = userService.getState(chatId);

        return switch (state) {
            case IDLE -> handleIdle(chatId);
            case WAITING_UNTRACK_LINK ->
                handleWaitingLink(chatId, update.message().text());
            case WAITING_LINK, WAITING_TAGS, WAITING_FILTERS -> new SendMessage(chatId, "Please use /cancel first");
        };
    }

    private SendMessage handleIdle(long chatId) {
        userService.setState(chatId, UserState.WAITING_UNTRACK_LINK);
        return new SendMessage(chatId, message());
    }

    private SendMessage handleWaitingLink(long chatId, String text) {
        if (!text.startsWith("http://") && !text.startsWith("https://")) {
            return new SendMessage(chatId, "Invalid link. Please send a valid URL");
        }
        try {
            URI uri = URI.create(text);
            userService.resetSession(chatId);
            scrapperGrpcClient.untrackLink(chatId, uri.toString());
            return new SendMessage(chatId, "Link " + uri + " is no longer tracked!");
        } catch (io.grpc.StatusRuntimeException e) {
            return switch (e.getStatus().getCode()) {
                case NOT_FOUND -> new SendMessage(chatId, "Link not found or chat not registered");
                default -> new SendMessage(chatId, "Error removing link. Please try again");
            };
        } catch (IllegalArgumentException e) {
            return new SendMessage(chatId, "Invalid link. Please send a valid URL");
        }
    }

    @Override
    public Set<UserState> handledStates() {
        return Set.of(UserState.WAITING_UNTRACK_LINK);
    }
}
