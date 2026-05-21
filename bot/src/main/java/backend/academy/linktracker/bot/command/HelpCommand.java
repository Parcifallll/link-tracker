package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HelpCommand implements Command {

    private final List<Command> commands;

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public SendMessage handle(Update update) {

        return new SendMessage(update.message().chat().id(), message());
    }

    public String description() {
        return "Available commands";
    }

    @Override
    public String message() {
        String helpText = commands.stream()
                .filter(c -> !c.command().isEmpty())
                .map(c -> c.command() + " — " + c.description())
                .collect(Collectors.joining("\n"));
        return "Available commands: \n" + helpText;
    }
}
