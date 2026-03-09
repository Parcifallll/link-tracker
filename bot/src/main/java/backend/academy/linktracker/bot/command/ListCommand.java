package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.grpc.ScrapperGrpcClient;
import backend.academy.linktracker.grpc.LinkItem;
import backend.academy.linktracker.grpc.ListLinksResponse;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListCommand implements Command {

    private final ScrapperGrpcClient scrapperGrpcClient;

    @Override
    public String command() {
        return "/list";
    }

    @Override
    public String description() {
        return "Show tracked links";
    }

    @Override
    public String message() {
        return "You have no tracked links";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        String text = update.message().text();

        // extract optional tag filter: "/list work" -> tagFilter = "work"
        String[] parts = text.split("\\s+", 2);
        String tagFilter = parts.length > 1 ? parts[1].trim() : null;

        try {
            ListLinksResponse response = scrapperGrpcClient.listLinks(chatId, tagFilter);
            List<LinkItem> links = response.getLinksList();

            if (links.isEmpty()) {
                return new SendMessage(chatId, message());
            }

            String formatted = links.stream()
                .map(link -> "• " + link.getUrl()
                    + (link.getTagsList().isEmpty() ? "" : " [" + String.join(", ", link.getTagsList()) + "]"))
                .collect(Collectors.joining("\n"));

            return new SendMessage(chatId, "Your tracked links:\n" + formatted);
        } catch (io.grpc.StatusRuntimeException e) {
            return switch (e.getStatus().getCode()) {
                case NOT_FOUND -> new SendMessage(chatId, "Chat not found. Please use /start first");
                default -> new SendMessage(chatId, "Error getting links. Please try again");
            };
        }
    }
}
