package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;

@Component
public class UnknownCommand implements Command {

    @Override
    public String command() {
        // fallback
        return "";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        return new SendMessage(chatId, message());
    }

    @Override
    public String description() {
        return "Unknown command";
    }

    @Override
    public String message() {
        return "Unknown command. Type /help for getting available commands.";
    }
}
