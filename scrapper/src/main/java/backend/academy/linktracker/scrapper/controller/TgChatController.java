package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/tg-chat")
@RequiredArgsConstructor
public class TgChatController {

    private final ChatService chatService;

    @PostMapping("/{id}")
    public ResponseEntity<Void> register(@PathVariable long id) {
        MDC.put("chatId", String.valueOf(id));
        try {
            log.atInfo().log("Registering chat");
            chatService.register(id);
            return ResponseEntity.ok().build();
        } finally {
            MDC.clear();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        MDC.put("chatId", String.valueOf(id));
        try {
            log.atInfo().log("Deleting chat");
            chatService.delete(id);
            return ResponseEntity.ok().build();
        } finally {
            MDC.clear();
        }
    }
}
