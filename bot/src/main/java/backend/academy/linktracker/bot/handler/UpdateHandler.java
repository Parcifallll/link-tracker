package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.UnknownCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateHandler {

    private final List<Command> commands;
    private final UnknownCommand unknownCommand;

    public SendMessage handle(Update update) {
        if (update.message() == null || update.message().text() == null) {
            return null;
        }

        // extract command
        String text = update.message().text().split("[@\\s]")[0];
        long chatId = update.message().chat().id();

        log.atInfo().addKeyValue("command", text).addKeyValue("chatId", chatId).log("Received command");

        Command command = commands.stream()
                .filter(c -> c.command().equals(text))
                .findFirst()
                .orElse(unknownCommand);

        return command.handle(update);
    }
}
