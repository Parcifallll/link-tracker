package backend.academy.linktracker.scrapper.grpc;

import backend.academy.linktracker.grpc.BotUpdateServiceGrpc;
import backend.academy.linktracker.grpc.SendUpdateRequest;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.service.MessageSender;
import backend.academy.linktracker.scrapper.service.UpdateInfo;
import io.grpc.ManagedChannel;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GrpcMessageSender implements MessageSender {

    private final BotUpdateServiceGrpc.BotUpdateServiceBlockingStub stub;

    public GrpcMessageSender(GrpcChannelFactory channelFactory) {
        ManagedChannel channel = channelFactory.createChannel("bot");
        this.stub = BotUpdateServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public void sendUpdate(Link link, UpdateInfo updateInfo, List<Long> chatIds) {
        String formattedMessage = formatUpdateMessage(link, updateInfo);

        SendUpdateRequest request = SendUpdateRequest.newBuilder()
                .setId(link.getId())
                .setUrl(link.getUrl().toString())
                .setDescription(formattedMessage)
                .addAllTgChatIds(chatIds)
                .build();

        try {
            stub.sendUpdate(request);
            log.info("Update sent for link: {}", link.getUrl());
        } catch (Exception e) {
            log.error("Failed to send update via gRPC for link: {}", link.getUrl(), e);
        }
    }

    @Override
    public void sendError(Link link, String errorMessage, List<Long> chatIds) {
        String message = String.format("Failed to check link: %s%nError: %s", link.getUrl(), errorMessage);

        SendUpdateRequest request = SendUpdateRequest.newBuilder()
                .setId(link.getId())
                .setUrl(link.getUrl().toString())
                .setDescription(message)
                .addAllTgChatIds(chatIds)
                .build();

        try {
            stub.sendUpdate(request);
        } catch (Exception e) {
            log.error("Failed to send error via gRPC for link: {}", link.getUrl(), e);
        }
    }

    private String formatUpdateMessage(Link link, UpdateInfo updateInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Обновление по ссылке: ").append(link.getUrl()).append("\n\n");
        sb.append("* ").append(updateInfo.linkTitle()).append("\n\n");

        updateInfo.itemsByType().forEach((type, items) -> {
            String typeLabel =
                    switch (type) {
                        case GITHUB_ISSUE -> "Новые Issues";
                        case GITHUB_PR -> "Новые Pull Requests";
                        case STACKOVERFLOW_ANSWER -> "Новые ответы";
                        case STACKOVERFLOW_COMMENT -> "Новые комментарии";
                    };

            sb.append(typeLabel).append(":\n");
            items.forEach(item -> {
                sb.append("* ").append(item.title()).append("\n");
                sb.append("    - ").append(item.author()).append("\n");
                sb.append("    - ").append(item.createdAt()).append("\n");
                if (!item.preview().isEmpty()) {
                    sb.append("    - ").append(item.preview()).append("\n");
                }
                sb.append("\n");
            });
        });

        return sb.toString();
    }
}
