package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.UnknownCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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

        long chatId = update.message().chat().id();
        String text = update.message().text().split("[@\\s]")[0];

        MDC.put("chatId", String.valueOf(chatId));
        MDC.put("command", text);
        try {
            log.atInfo().log("Received command");

            Command command = commands.stream()
                .filter(c -> c.command().equals(text))
                .findFirst()
                .orElseGet(() -> commands.stream()// check if user is in the dialog (e.g. after /track)
                    .filter(Command::isStateful)
                    .findFirst()
                    .orElse(unknownCommand));

            return command.handle(update);
        } finally {
            MDC.clear();
        }
    }
}
