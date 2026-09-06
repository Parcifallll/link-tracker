package backend.academy.linktracker.ai.configuration;

import backend.academy.linktracker.ai.dto.RawLinkUpdate;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Bean
    public NewTopic rawUpdatesTopic() {
        return TopicBuilder.name("link.raw-updates").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic processedUpdatesTopic() {
        return TopicBuilder.name("link.processed-updates")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RawLinkUpdate> kafkaListenerContainerFactory(
            ConsumerFactory<String, RawLinkUpdate> consumerFactory) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, RawLinkUpdate>();
        factory.setConsumerFactory(consumerFactory);

        factory.setCommonErrorHandler(new DefaultErrorHandler(
                (record, ex) -> log.error("Failed to process offset={}: {}", record.offset(), ex.getMessage()),
                new FixedBackOff(1000L, 2L)));

        return factory;
    }
}
