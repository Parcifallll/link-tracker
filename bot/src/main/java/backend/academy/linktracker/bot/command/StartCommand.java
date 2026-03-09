package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.grpc.ScrapperGrpcClient;
import backend.academy.linktracker.bot.service.UserService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommand implements Command {

    private final UserService userService;
    private final ScrapperGrpcClient scrapperGrpcClient;

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public String description() {
        return "Start the bot";
    }

    @Override
    public String message() {
        return "Welcome! Type /help for getting available commands.";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        userService.registerUser(chatId);
        try {
            scrapperGrpcClient.registerChat(chatId);
        } catch (Exception e) {
            // chat may already exist on scrapper restart
        }
        return new SendMessage(chatId, message());
    }
}
