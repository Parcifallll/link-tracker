package backend.academy.linktracker.scrapper.grpc;

import backend.academy.linktracker.grpc.DeleteChatRequest;
import backend.academy.linktracker.grpc.DeleteChatResponse;
import backend.academy.linktracker.grpc.LinkItem;
import backend.academy.linktracker.grpc.ListLinksRequest;
import backend.academy.linktracker.grpc.ListLinksResponse;
import backend.academy.linktracker.grpc.RegisterChatRequest;
import backend.academy.linktracker.grpc.RegisterChatResponse;
import backend.academy.linktracker.grpc.ScrapperServiceGrpc;
import backend.academy.linktracker.grpc.TrackLinkRequest;
import backend.academy.linktracker.grpc.TrackLinkResponse;
import backend.academy.linktracker.grpc.UntrackLinkRequest;
import backend.academy.linktracker.grpc.UntrackLinkResponse;
import backend.academy.linktracker.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.service.ChatService;
import backend.academy.linktracker.scrapper.service.LinkService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ScrapperGrpcService extends ScrapperServiceGrpc.ScrapperServiceImplBase {

    private final ChatService chatService;
    private final LinkService linkService;

    @Override
    public void registerChat(RegisterChatRequest request, StreamObserver<RegisterChatResponse> responseObserver) {
        MDC.put("chatId", String.valueOf(request.getChatId()));
        try {
            log.atInfo().log("Registering chat via gRPC");
            chatService.register(request.getChatId());
            responseObserver.onNext(
                    RegisterChatResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        } catch (ChatAlreadyExistsException e) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("RegisterChat error");
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal error").asRuntimeException());
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void deleteChat(DeleteChatRequest request, StreamObserver<DeleteChatResponse> responseObserver) {
        MDC.put("chatId", String.valueOf(request.getChatId()));
        try {
            log.atInfo().log("Deleting chat via gRPC");
            chatService.delete(request.getChatId());
            responseObserver.onNext(
                    DeleteChatResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("DeleteChat error");
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal error").asRuntimeException());
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void trackLink(TrackLinkRequest request, StreamObserver<TrackLinkResponse> responseObserver) {
        MDC.put("chatId", String.valueOf(request.getChatId()));
        MDC.put("url", request.getUrl());
        try {
            log.atInfo().log("Tracking link via gRPC");
            linkService.add(
                    request.getChatId(), URI.create(request.getUrl()), request.getTagsList(), request.getFiltersList());
            responseObserver.onNext(TrackLinkResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Link is now being tracked")
                    .build());
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (LinkAlreadyExistsException e) {
            responseObserver.onError(
                    Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("TrackLink error");
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal error").asRuntimeException());
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void untrackLink(UntrackLinkRequest request, StreamObserver<UntrackLinkResponse> responseObserver) {
        MDC.put("chatId", String.valueOf(request.getChatId()));
        MDC.put("url", request.getUrl());
        try {
            log.atInfo().log("Untracking link via gRPC");
            linkService.remove(request.getChatId(), URI.create(request.getUrl()));
            responseObserver.onNext(UntrackLinkResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Link is no longer tracked")
                    .build());
            responseObserver.onCompleted();
        } catch (ChatNotFoundException | LinkNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("UntrackLink error");
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal error").asRuntimeException());
        } finally {
            MDC.clear();
        }
    }

    @Override
    public void listLinks(ListLinksRequest request, StreamObserver<ListLinksResponse> responseObserver) {
        MDC.put("chatId", String.valueOf(request.getChatId()));
        try {
            log.atInfo().log("Listing links via gRPC");
            List<Link> links = linkService.findAll(request.getChatId());

            // apply tag filter if present
            String tagFilter = request.getTagFilter().isEmpty() ? null : request.getTagFilter();
            if (tagFilter != null) {
                links = links.stream()
                        .filter(l -> l.getTags() != null && l.getTags().contains(tagFilter))
                        .toList();
            }

            List<LinkItem> items = links.stream()
                    .map(l -> LinkItem.newBuilder()
                            .setId(l.getId())
                            .setUrl(l.getUrl().toString())
                            .addAllTags(l.getTags() != null ? l.getTags() : List.of())
                            .addAllFilters(l.getFilters() != null ? l.getFilters() : List.of())
                            .build())
                    .toList();

            responseObserver.onNext(ListLinksResponse.newBuilder()
                    .addAllLinks(items)
                    .setMessage(items.isEmpty() ? "No tracked links" : "OK")
                    .build());
            responseObserver.onCompleted();
        } catch (ChatNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("ListLinks error");
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal error").asRuntimeException());
        } finally {
            MDC.clear();
        }
    }
}
