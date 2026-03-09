package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.grpc.ScrapperGrpcClient;
import backend.academy.linktracker.bot.model.UserState;
import backend.academy.linktracker.bot.service.UserService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackCommand implements Command {

    private final UserService userService;
    private final ScrapperGrpcClient scrapperGrpcClient;

    @Override
    public String command() {
        return "/track";
    }

    @Override
    public String description() {
        return "Start tracking a link";
    }

    @Override
    public String message() {
        return "Send me a link to track";
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public Set<UserState> handledStates() {
        return Set.of(UserState.WAITING_LINK, UserState.WAITING_TAGS, UserState.WAITING_FILTERS);
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        UserState state = userService.getState(chatId);

        return switch (state) {
            case IDLE -> handleIdle(chatId);
            case WAITING_LINK -> handleWaitingLink(chatId, update.message().text());
            case WAITING_TAGS -> handleWaitingTags(chatId, update.message().text());
            case WAITING_FILTERS ->
                handleWaitingFilters(chatId, update.message().text());
            case WAITING_UNTRACK_LINK -> new SendMessage(chatId, "Please use /cancel first");
        };
    }

    private SendMessage handleIdle(long chatId) {
        userService.setState(chatId, UserState.WAITING_LINK);
        return new SendMessage(chatId, message());
    }

    private SendMessage handleWaitingLink(long chatId, String text) {
        if (!text.startsWith("http://") && !text.startsWith("https://")) {
            return new SendMessage(chatId, "Invalid link. Please send a valid URL starting with http:// or https://");
        }
        try {
            URI uri = URI.create(text);
            if (scrapperGrpcClient.linkExists(chatId, uri.toString())) {
                userService.resetSession(chatId);
                return new SendMessage(chatId, "Ссылка уже отслеживается");
            }
            userService.setPendingLink(chatId, uri);
            userService.setState(chatId, UserState.WAITING_TAGS);
            return new SendMessage(chatId, "Send tags separated by comma, or \"skip\"");
        } catch (IllegalArgumentException e) {
            return new SendMessage(chatId, "Invalid link. Please send a valid URL");
        }
    }

    private SendMessage handleWaitingTags(long chatId, String text) {
        List<String> tags = text.equalsIgnoreCase("skip")
                ? List.of()
                : Arrays.stream(text.split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .toList();

        userService.setTags(chatId, tags);
        userService.setState(chatId, UserState.WAITING_FILTERS);
        return new SendMessage(chatId, "Send filters separated by comma, or \"skip\"");
    }

    private SendMessage handleWaitingFilters(long chatId, String text) {
        List<String> filters = text.equalsIgnoreCase("skip")
                ? List.of()
                : Arrays.stream(text.split(","))
                        .map(String::trim)
                        .filter(f -> !f.isEmpty())
                        .toList();

        URI link = userService.getPendingLink(chatId);
        List<String> tags = userService.getTags(chatId);
        userService.setFilters(chatId, filters);
        userService.resetSession(chatId);

        try {
            scrapperGrpcClient.trackLink(chatId, link.toString(), tags, filters);
            return new SendMessage(chatId, "Link " + link + " is now being tracked!");
        } catch (io.grpc.StatusRuntimeException e) {
            return switch (e.getStatus().getCode()) {
                case ALREADY_EXISTS -> new SendMessage(chatId, "Link is already tracked");
                case NOT_FOUND -> new SendMessage(chatId, "Chat not found. Please use /start first");
                default -> new SendMessage(chatId, "Error adding link. Please try again");
            };
        }
    }
}
