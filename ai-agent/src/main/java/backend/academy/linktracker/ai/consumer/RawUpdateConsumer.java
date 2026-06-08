package backend.academy.linktracker.ai.consumer;

import backend.academy.linktracker.ai.dto.RawLinkUpdate;
import backend.academy.linktracker.ai.service.UpdateProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RawUpdateConsumer {

    private final UpdateProcessingService processingService;

    @KafkaListener(
            topics = "link.raw-updates",
            groupId = "ai-agent-consumer-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(RawLinkUpdate update) {
        MDC.put("updateId", String.valueOf(update.id()));
        try {
            log.info("Received raw update id={}", update.id());
            processingService.process(update);
        } finally {
            MDC.clear();
        }
    }
}
