package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.grpc.ScrapperGrpcClient;
import backend.academy.linktracker.bot.service.UserService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
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
            log.atDebug().addKeyValue("error", e.getMessage()).log("Chat already exists on scrapper");
        }
        return new SendMessage(chatId, message());
    }
}
