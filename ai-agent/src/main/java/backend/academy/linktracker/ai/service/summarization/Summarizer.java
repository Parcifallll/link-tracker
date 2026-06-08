package backend.academy.linktracker.ai.service.summarization;

import backend.academy.linktracker.ai.properties.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Summarizer {

    private final AiAgentProperties properties;

    public String summarize(String text) {
        int threshold = properties.summarization().threshold();
        return text.substring(0, threshold) + "...";
    }
}
