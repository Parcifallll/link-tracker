package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.service.UserService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommand implements Command {

    private final UserService userService;

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        userService.registerUser(chatId);
        return new SendMessage(chatId, message());
    }

    @Override
    public String description() {
        return "Start the bot";
    }

    @Override
    public String message() {
        return "Welcome! Type /help for getting available commands.";
    }
}
