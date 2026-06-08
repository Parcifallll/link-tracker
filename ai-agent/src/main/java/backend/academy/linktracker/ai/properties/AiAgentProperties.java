package backend.academy.linktracker.ai.properties;

import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "ai-agent")
@Validated
public record AiAgentProperties(Filtering filtering, Summarization summarization) {

    public AiAgentProperties {
        if (filtering == null) filtering = new Filtering(List.of(), List.of(), 0);
        if (summarization == null) summarization = new Summarization(500);
    }

    public record Filtering(
            List<String> stopWords,
            List<String> excludedAuthors,
            @Min(0) int minLength) {

        public Filtering {
            if (stopWords == null) stopWords = List.of();
            if (excludedAuthors == null) excludedAuthors = List.of();
        }
    }

    public record Summarization(@Min(1) int threshold) {}
}
