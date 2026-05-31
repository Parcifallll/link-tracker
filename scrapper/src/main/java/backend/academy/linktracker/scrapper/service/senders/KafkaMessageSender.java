package backend.academy.linktracker.scrapper.service.senders;

import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import backend.academy.linktracker.scrapper.service.updates.dto.LinkSourceType;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaMessageSender implements MessageSender {

    private final org.springframework.kafka.core.KafkaTemplate<String, LinkUpdate> kafkaTemplate;
    private final KafkaProperties kafkaProperties;

    @Override
    public void sendUpdate(Link link, UpdateInfo updateInfo, List<Long> chatIds) {
        LinkUpdate update = new LinkUpdate(link.getId(), link.getUrl(), updateInfo.linkTitle(), chatIds);
        send(link.getUrl().toString(), String.valueOf(link.getId()), update);
    }

    @Override
    public void sendError(Link link, String errorMessage, List<Long> chatIds) {
        LinkUpdate update = new LinkUpdate(link.getId(), link.getUrl(), "Error: " + errorMessage, chatIds);
        send(link.getUrl().toString(), String.valueOf(link.getId()), update);
    }

    private void send(String url, String key, LinkUpdate update) {
        String topic = determineTopic(url);
        try {
            kafkaTemplate.send(topic, key, update);
            log.atDebug().addKeyValue("topic", topic).addKeyValue("key", key).log("Sent to Kafka successfully");
        } catch (Exception e) {
            log.atWarn()
                    .addKeyValue("topic", topic)
                    .addKeyValue("key", key)
                    .addKeyValue("error", e.getMessage())
                    .log("Failed to send to Kafka");
            throw new RuntimeException("Kafka send failed for url: " + url, e);
        }
    }

    private String determineTopic(String url) {
        return LinkSourceType.getTopic(url, kafkaProperties.getTopics().getFallback());
    }
}
