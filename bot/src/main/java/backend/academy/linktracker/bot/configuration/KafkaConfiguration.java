package backend.academy.linktracker.bot.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Bean
    public NewTopic githubUpdatesTopic() {
        return TopicBuilder.name("github-updates").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic stackoverflowUpdatesTopic() {
        return TopicBuilder.name("stackoverflow-updates")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
