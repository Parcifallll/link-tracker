package backend.academy.linktracker.scrapper.service.senders;

import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

// Kafka is primary
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class FallbackMessageSender implements MessageSender {

    private final KafkaMessageSender kafkaSender;
    private final GrpcMessageSender grpcSender;

    @Override
    public void sendUpdate(Link link, UpdateInfo updateInfo, List<Long> chatIds) {
        try {
            kafkaSender.sendUpdate(link, updateInfo, chatIds);
        } catch (Exception kafkaEx) {
            log.atWarn()
                    .addKeyValue("url", link.getUrl().toString())
                    .addKeyValue("kafkaError", kafkaEx.getMessage())
                    .log("Kafka unavailable, falling back to gRPC");
            grpcSender.sendUpdate(link, updateInfo, chatIds);
        }
    }

    @Override
    public void sendError(Link link, String errorMessage, List<Long> chatIds) {
        try {
            kafkaSender.sendError(link, errorMessage, chatIds);
        } catch (Exception kafkaEx) {
            log.atWarn()
                    .addKeyValue("url", link.getUrl().toString())
                    .addKeyValue("kafkaError", kafkaEx.getMessage())
                    .log("Kafka unavailable, falling back to gRPC for error notification");
            grpcSender.sendError(link, errorMessage, chatIds);
        }
    }
}
