package backend.academy.linktracker.scrapper.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    private Topics topics;

    @Getter
    @Setter
    public static class Topics {
        private String githubUpdates;
        private String stackoverflowUpdates;
        private String fallback = "unknown-updates";
    }
}
