package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.service.UserService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CancelCommand implements Command {

    private final UserService userService;

    @Override
    public String command() {
        return "/cancel";
    }

    @Override
    public String description() {
        return "Cancel current operation";
    }

    @Override
    public String message() {
        return "Operation cancelled";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        userService.resetSession(chatId);
        return new SendMessage(chatId, message());
    }
}
