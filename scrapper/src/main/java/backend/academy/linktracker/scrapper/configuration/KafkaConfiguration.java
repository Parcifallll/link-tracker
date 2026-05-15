package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfiguration {

    private final KafkaProperties kafkaProperties;

    @Bean
    public NewTopic githubUpdatesTopic() {
        return new NewTopic(kafkaProperties.getTopics().getGithubUpdates(), 3, (short) 1);
    }

    @Bean
    public NewTopic stackoverflowUpdatesTopic() {
        return new NewTopic(kafkaProperties.getTopics().getStackoverflowUpdates(), 3, (short) 1);
    }

    @Bean
    public NewTopic fallbackUpdatesTopic() {
        return new NewTopic(kafkaProperties.getTopics().getFallback(), 3, (short) 1);
    }
}


