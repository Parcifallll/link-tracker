package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty(name = "app.notification.type", havingValue = "kafka", matchIfMissing = true)
public class KafkaMessageSender implements MessageSender {

    private final KafkaTemplate<String, LinkUpdate> kafkaTemplate;
    private final KafkaProperties kafkaProperties;

    @Override
    public void sendUpdate(Link link, UpdateInfo updateInfo, List<Long> chatIds) {
        LinkUpdate update = new LinkUpdate(
            link.getId(),
            link.getUrl(),
            updateInfo.linkTitle(),
            chatIds
        );

        String topic = determineTopic(link.getUrl().toString());
        kafkaTemplate.send(topic, String.valueOf(link.getId()), update);
        log.atDebug().addKeyValue("topic", topic).addKeyValue("linkId", link.getId()).log("Sent update to Kafka");
    }

    @Override
    public void sendError(Link link, String errorMessage, List<Long> chatIds) {
        LinkUpdate update = new LinkUpdate(
            link.getId(),
            link.getUrl(),
            "Error: " + errorMessage,
            chatIds
        );

        String topic = determineTopic(link.getUrl().toString());
        kafkaTemplate.send(topic, String.valueOf(link.getId()), update);
        log.atError().addKeyValue("topic", topic).addKeyValue("linkId", link.getId()).log("Sent error to Kafka");
    }
    private String determineTopic(String url) {
        return LinkSourceType.getTopic(url, kafkaProperties.getTopics().getFallback());
    }
}

