package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.UnknownCommand;
import backend.academy.linktracker.bot.model.UserState;
import backend.academy.linktracker.bot.service.UserService;
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
    private final UserService userService;

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

            UserState state = userService.getState(chatId);

            // if user is in dialog and sends any command -> cancel session
            if (state != UserState.IDLE && text.startsWith("/") && !text.equals("/cancel")) {
                userService.resetSession(chatId);
            }

            Command command = commands.stream()
                .filter(c -> c.command().equals(text))
                .findFirst()
                .orElseGet(() -> commands.stream()
                    .filter(c -> c.handledStates().contains(userService.getState(chatId)))
                    .findFirst()
                    .orElse(unknownCommand));

            // check registration before executing
            if (command.requiresRegistration() && userService.isNewUser(chatId)) {
                return new SendMessage(chatId, "Please use /start first to register");
            }

            return command.handle(update);
        } finally {
            MDC.clear();
        }
    }
}
