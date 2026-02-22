package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand implements Command {

    private static final String HELP_TEXT = """
            Available:
            /start — start the bot
            /help — available commands
            """;

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        return new SendMessage(chatId, HELP_TEXT);
    }
}
