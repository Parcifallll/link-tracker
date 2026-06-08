package backend.academy.linktracker.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.ai.dto.ProcessedLinkUpdate;
import backend.academy.linktracker.ai.dto.RawLinkUpdate;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.ai.properties.AiAgentProperties.Filtering;
import backend.academy.linktracker.ai.properties.AiAgentProperties.Summarization;
import backend.academy.linktracker.ai.service.UpdateFilterService;
import backend.academy.linktracker.ai.service.UpdateProcessingService;
import backend.academy.linktracker.ai.service.summarization.Summarizer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class UpdateProcessingServiceTest {

    @Mock
    private UpdateFilterService filterService;

    @Mock
    private Summarizer summarizer;

    @Mock
    private KafkaTemplate<String, ProcessedLinkUpdate> kafkaTemplate;

    private static final int THRESHOLD = 100;
    private UpdateProcessingService processingService;

    @BeforeEach
    void setUp() {
        var properties = new AiAgentProperties(new Filtering(List.of(), List.of(), 0), new Summarization(THRESHOLD));
        processingService = new UpdateProcessingService(filterService, summarizer, kafkaTemplate, properties);
    }

    @Test
    void longDescription_summarizationInvoked() {
        String longText = "a".repeat(THRESHOLD + 1);
        RawLinkUpdate update = new RawLinkUpdate(1L, longText, "alice", List.of(1L));

        when(filterService.passes(update)).thenReturn(true);
        when(summarizer.summarize(longText)).thenReturn("summarized");

        processingService.process(update);

        verify(summarizer).summarize(longText);
        verify(kafkaTemplate).send(eq(UpdateProcessingService.TOPIC), anyString(), any(ProcessedLinkUpdate.class));
    }

    @Test
    void shortDescription_summarizationSkipped() {
        String shortText = "a".repeat(THRESHOLD);
        RawLinkUpdate update = new RawLinkUpdate(1L, shortText, "alice", List.of(1L));

        when(filterService.passes(update)).thenReturn(true);

        processingService.process(update);

        verify(summarizer, never()).summarize(anyString());
        verify(kafkaTemplate).send(eq(UpdateProcessingService.TOPIC), anyString(), any(ProcessedLinkUpdate.class));
    }

    @Test
    void filteredUpdate_notPublished() {
        RawLinkUpdate update = new RawLinkUpdate(1L, "spam content here", "bot-user", List.of(1L));

        when(filterService.passes(update)).thenReturn(false);

        processingService.process(update);

        verify(summarizer, never()).summarize(anyString());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void longDescription_publishedWithSummarizedText() {
        String longText = "a".repeat(THRESHOLD + 50);
        RawLinkUpdate update = new RawLinkUpdate(1L, longText, "alice", List.of(1L));

        when(filterService.passes(update)).thenReturn(true);
        when(summarizer.summarize(longText)).thenReturn("short summary");

        processingService.process(update);

        verify(kafkaTemplate).send(eq(UpdateProcessingService.TOPIC), anyString(), argThat(p -> p.description()
                .equals("short summary")));
    }

    @Test
    void processedUpdate_hasPriorityHigh() {
        RawLinkUpdate update = new RawLinkUpdate(1L, "a".repeat(THRESHOLD), "alice", List.of(1L));

        when(filterService.passes(update)).thenReturn(true);

        processingService.process(update);

        verify(kafkaTemplate)
                .send(eq(UpdateProcessingService.TOPIC), anyString(), argThat(p -> "HIGH".equals(p.priority())));
    }
}
