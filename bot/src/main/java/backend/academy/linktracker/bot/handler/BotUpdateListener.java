package backend.academy.linktracker.bot.handler;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotUpdateListener implements InitializingBean {

    private final TelegramBot bot;
    private final UpdateHandler handler;

    @Override
    public void afterPropertiesSet() {
        bot.execute(new SetMyCommands(
                new BotCommand("/start", "Start the bot"), new BotCommand("/help", "Available commands")));

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
}
