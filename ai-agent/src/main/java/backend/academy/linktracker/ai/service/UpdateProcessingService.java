package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.ai.dto.RawLinkUpdate;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.ai.service.summarization.Summarizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateProcessingService {

    public static final String TOPIC = "link.processed-updates";

    private final UpdateFilterService filterService;
    private final Summarizer summarizer;
    private final KafkaTemplate<String, ProcessedLinkUpdate> kafkaTemplate;
    private final AiAgentProperties properties;

    public void process(RawLinkUpdate update) {
        if (!filterService.passes(update)) {
            log.info("Update {} filtered out", update.id());
            return;
        }

        int threshold = properties.summarization().threshold();
        String description = update.description().length() > threshold
                ? summarizer.summarize(update.description())
                : update.description();

        ProcessedLinkUpdate processed = ProcessedLinkUpdate.from(update, description);
        kafkaTemplate.send(TOPIC, String.valueOf(processed.id()), processed);
        log.info("Update {} processed and published", update.id());
    }
}
