package backend.academy.linktracker.scrapper.grpc;

import backend.academy.linktracker.grpc.BotUpdateServiceGrpc;
import backend.academy.linktracker.grpc.SendUpdateRequest;
import backend.academy.linktracker.grpc.UpdateItem;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.service.MessageSender;
import backend.academy.linktracker.scrapper.service.UpdateInfo;
import io.grpc.ManagedChannel;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.notification.type", havingValue = "grpc")
public class GrpcMessageSender implements MessageSender {

    private final BotUpdateServiceGrpc.BotUpdateServiceBlockingStub stub;

    public GrpcMessageSender(GrpcChannelFactory channelFactory) {
        ManagedChannel channel = channelFactory.createChannel("bot");
        this.stub = BotUpdateServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void sendUpdate(Link link, UpdateInfo updateInfo, List<Long> chatIds) {
        SendUpdateRequest request = SendUpdateRequest.newBuilder()
                .setId(link.getId())
                .setUrl(link.getUrl().toString())
                .setTitle(getLinkTitle(updateInfo, link))
                .addAllTgChatIds(chatIds)
                .addAllUpdates(buildProtoUpdates(updateInfo))
                .build();

        try {
            stub.sendUpdate(request);
            log.info("Successfully sent update via gRPC for link: {}", link.getUrl());
        } catch (Exception e) {
            log.error("Failed to send update via gRPC for link: {}", link.getUrl(), e);
        }
    }

    @Override
    public void sendError(Link link, String errorMessage, List<Long> chatIds) {
        SendUpdateRequest request = SendUpdateRequest.newBuilder()
                .setId(link.getId())
                .setUrl(link.getUrl().toString())
                .setTitle("Error checking link")
                .setError(errorMessage)
                .addAllTgChatIds(chatIds)
                .build();

        try {
            stub.sendUpdate(request);
        } catch (Exception e) {
            log.error("Failed to send error via gRPC for link: {}", link.getUrl(), e);
        }
    }

    private String getLinkTitle(UpdateInfo updateInfo, Link link) {
        if (updateInfo.linkTitle() != null && !updateInfo.linkTitle().isBlank()) {
            return updateInfo.linkTitle();
        }
        return link.getUrl().toString();
    }

    private List<UpdateItem> buildProtoUpdates(UpdateInfo updateInfo) {
        return updateInfo.itemsByType().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(item -> UpdateItem.newBuilder()
                        .setType(entry.getKey().name())
                        .setTitle(defaultIfNull(item.title()))
                        .setAuthor(defaultIfNull(item.author()))
                        .setCreatedAt(item.createdAt().toString())
                        .setPreview(defaultIfNull(item.preview()))
                        .build()))
                .toList();
    }

    private String defaultIfNull(String value) {
        return value != null ? value : "";
    }
}
