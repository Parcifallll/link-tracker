package backend.academy.linktracker.bot.handler;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaUpdateListener {

    private final TelegramBot bot;

    @KafkaListener(topics = {"github-updates", "stackoverflow-updates"}, groupId = "bot-consumer-group")
    public void handleUpdate(LinkUpdate update) {
        MDC.put("url", update.url().toString());
        try {
            log.atDebug().log("Received update via Kafka");
            update.tgChatIds().forEach(chatId -> {
                MDC.put("chatId", String.valueOf(chatId));
                String text = "Update on link: " + update.url();
                bot.execute(new SendMessage(chatId, text));
            });
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("Failed to handle Kafka update");
        } finally {
            MDC.clear();
        }
    }
}
