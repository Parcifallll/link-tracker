package backend.academy.linktracker.ai;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.ai.properties.AiAgentProperties.Filtering;
import backend.academy.linktracker.ai.properties.AiAgentProperties.Summarization;
import backend.academy.linktracker.ai.service.summarization.Summarizer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SummarizerTest {

    private static final int THRESHOLD = 100;
    private Summarizer summarizer;

    @BeforeEach
    void setUp() {
        var properties = new AiAgentProperties(new Filtering(List.of(), List.of(), 0), new Summarization(THRESHOLD));
        summarizer = new Summarizer(properties);
    }

    @Test
    void longText_isTruncatedToThreshold() {
        String result = summarizer.summarize("a".repeat(300));
        assertThat(result).hasSize(THRESHOLD + 3);
        assertThat(result).endsWith("...");
    }

    @Test
    void longText_resultDoesNotContainFullOriginal() {
        assertThat(summarizer.summarize("x".repeat(300)).length()).isLessThan(300);
    }

    @Test
    void longText_truncatedAtExactThreshold() {
        assertThat(summarizer.summarize("a".repeat(300))).startsWith("a".repeat(THRESHOLD));
    }

    @Test
    void textAtThreshold_truncatesToThresholdPlusEllipsis() {
        assertThat(summarizer.summarize("b".repeat(THRESHOLD))).isEqualTo("b".repeat(THRESHOLD) + "...");
    }
}
