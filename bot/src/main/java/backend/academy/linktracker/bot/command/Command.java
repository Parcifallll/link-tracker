package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.model.UserState;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Set;

public interface Command {

    String command();

    SendMessage handle(Update update);

    String description();

    String message();

    default boolean isStateful() {
        return false;
    }

    default Set<UserState> handledStates() {
        return Set.of();
    }

    // "start" command
    default boolean requiresRegistration() {
        return false;
    }
}
