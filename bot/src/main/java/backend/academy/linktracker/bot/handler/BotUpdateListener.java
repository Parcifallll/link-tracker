package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.command.Command;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import jakarta.annotation.PreDestroy;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotUpdateListener {

    private final TelegramBot bot;
    private final UpdateHandler handler;
    private final List<Command> commands;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        BotCommand[] menu = commands.stream()
                .filter(c -> !c.command().isEmpty())
                .map(c -> new BotCommand(c.command(), c.description()))
                .toArray(BotCommand[]::new);

        bot.execute(new SetMyCommands(menu));

        bot.setUpdatesListener(
                updates -> {
                    updates.forEach(update -> {
                        SendMessage message = handler.handle(update);
                        if (message != null) {
                            bot.execute(message);
                        }
                    });
                    return UpdatesListener.CONFIRMED_UPDATES_ALL;
                },
                exception -> log.atError()
                        .addKeyValue("cause", exception.getMessage())
                        .log("Updates listener error"));
    }

    @PreDestroy
    public void stop() {
        bot.removeGetUpdatesListener();
    }
}
