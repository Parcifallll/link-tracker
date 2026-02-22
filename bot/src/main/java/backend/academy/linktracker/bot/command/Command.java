package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;

public interface Command {

    // returns the supported command name (e.x. "/start")
    String command();

    SendMessage handle(Update update);
}
