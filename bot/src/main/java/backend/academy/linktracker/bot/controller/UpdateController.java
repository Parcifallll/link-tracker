package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/updates")
@RequiredArgsConstructor
public class UpdateController {

    private final TelegramBot bot;

    @PostMapping
    public ResponseEntity<Void> receiveUpdate(@Valid @RequestBody LinkUpdate update) {
        MDC.put("url", update.url().toString());
        try {
            log.atInfo().log("Received update via HTTP");
            update.tgChatIds().forEach(chatId -> {
                MDC.put("chatId", String.valueOf(chatId));
                String text = "Update on link: " + update.url();
                bot.execute(new SendMessage(chatId, text));
            });
            return ResponseEntity.ok().build();
        } finally {
            MDC.clear();
        }
    }
}
