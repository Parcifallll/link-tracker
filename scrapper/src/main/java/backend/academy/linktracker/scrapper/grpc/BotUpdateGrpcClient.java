package backend.academy.linktracker.scrapper.grpc;

import backend.academy.linktracker.grpc.BotUpdateServiceGrpc;
import backend.academy.linktracker.grpc.SendUpdateRequest;
import backend.academy.linktracker.scrapper.model.Link;
import io.grpc.ManagedChannel;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BotUpdateGrpcClient {

    private final BotUpdateServiceGrpc.BotUpdateServiceBlockingStub stub;

    public BotUpdateGrpcClient(GrpcChannelFactory channelFactory) {
        ManagedChannel channel = channelFactory.createChannel("bot");
        this.stub = BotUpdateServiceGrpc.newBlockingStub(channel);
    }

    public void sendUpdate(Link link, List<Long> chatIds) {
        MDC.put("url", link.getUrl().toString());
        try {
            log.atInfo().log("Sending update to bot via gRPC");
            SendUpdateRequest request = SendUpdateRequest.newBuilder()
                .setId(link.getId())
                .setUrl(link.getUrl().toString())
                .setDescription("Link updated: " + link.getUrl())
                .addAllTgChatIds(chatIds)
                .build();
            stub.sendUpdate(request);
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("Failed to send update to bot");
        } finally {
            MDC.clear();
        }
    }
}
