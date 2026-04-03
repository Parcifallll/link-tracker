package backend.academy.linktracker.bot.grpc;

import backend.academy.linktracker.grpc.DeleteChatRequest;
import backend.academy.linktracker.grpc.ListLinksRequest;
import backend.academy.linktracker.grpc.ListLinksResponse;
import backend.academy.linktracker.grpc.RegisterChatRequest;
import backend.academy.linktracker.grpc.ScrapperServiceGrpc;
import backend.academy.linktracker.grpc.TrackLinkRequest;
import backend.academy.linktracker.grpc.TrackLinkResponse;
import backend.academy.linktracker.grpc.UntrackLinkRequest;
import backend.academy.linktracker.grpc.UntrackLinkResponse;
import io.grpc.ManagedChannel;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScrapperGrpcClient {

    private final ScrapperServiceGrpc.ScrapperServiceBlockingStub stub;

    public ScrapperGrpcClient(GrpcChannelFactory channelFactory) {
        ManagedChannel channel = channelFactory.createChannel("scrapper");
        this.stub = ScrapperServiceGrpc.newBlockingStub(channel);
    }

    public void registerChat(long chatId) {
        MDC.put("chatId", String.valueOf(chatId));
        try {
            log.atInfo().log("Registering chat via gRPC");
            stub.registerChat(RegisterChatRequest.newBuilder().setChatId(chatId).build());
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("RegisterChat gRPC error");
            throw e;
        } finally {
            MDC.clear();
        }
    }

    public void deleteChat(long chatId) {
        MDC.put("chatId", String.valueOf(chatId));
        try {
            log.atInfo().log("Deleting chat via gRPC");
            stub.deleteChat(DeleteChatRequest.newBuilder().setChatId(chatId).build());
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("DeleteChat gRPC error");
            throw e;
        } finally {
            MDC.clear();
        }
    }

    public TrackLinkResponse trackLink(long chatId, String url, List<String> tags, List<String> filters) {
        MDC.put("chatId", String.valueOf(chatId));
        MDC.put("url", url);
        try {
            log.atInfo().log("Tracking link via gRPC");
            return stub.trackLink(TrackLinkRequest.newBuilder()
                    .setChatId(chatId)
                    .setUrl(url)
                    .addAllTags(tags)
                    .addAllFilters(filters)
                    .build());
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("TrackLink gRPC error");
            throw e;
        } finally {
            MDC.clear();
        }
    }

    public UntrackLinkResponse untrackLink(long chatId, String url) {
        MDC.put("chatId", String.valueOf(chatId));
        MDC.put("url", url);
        try {
            log.atInfo().log("Untracking link via gRPC");
            return stub.untrackLink(UntrackLinkRequest.newBuilder()
                    .setChatId(chatId)
                    .setUrl(url)
                    .build());
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("UntrackLink gRPC error");
            throw e;
        } finally {
            MDC.clear();
        }
    }

    public ListLinksResponse listLinks(long chatId, String tagFilter) {
        MDC.put("chatId", String.valueOf(chatId));
        try {
            log.atInfo().log("Listing links via gRPC");
            return stub.listLinks(ListLinksRequest.newBuilder()
                    .setChatId(chatId)
                    .setTagFilter(tagFilter != null ? tagFilter : "")
                    .build());
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("ListLinks gRPC error");
            throw e;
        } finally {
            MDC.clear();
        }
    }

    public boolean linkExists(long chatId, String url) {
        try {
            ListLinksResponse response = listLinks(chatId, null);
            return response.getLinksList().stream().anyMatch(l -> l.getUrl().equals(url));
        } catch (Exception e) {
            return false;
        }
    }
}
