package backend.academy.linktracker.bot.grpc;

import backend.academy.linktracker.grpc.BotUpdateServiceGrpc;
import backend.academy.linktracker.grpc.SendUpdateRequest;
import backend.academy.linktracker.grpc.SendUpdateResponse;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.grpc.server.service.GrpcService;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class BotUpdateGrpcService extends BotUpdateServiceGrpc.BotUpdateServiceImplBase {

    private final TelegramBot bot;

    @Override
    public void sendUpdate(SendUpdateRequest request, StreamObserver<SendUpdateResponse> responseObserver) {
        MDC.put("url", request.getUrl());
        try {
            log.atInfo().log("Received update via gRPC");
            request.getTgChatIdsList().forEach(chatId -> {
                MDC.put("chatId", String.valueOf(chatId));
                String text = "Update on link: " + request.getUrl()
                    + (request.getDescription().isEmpty() ? "" : "\n" + request.getDescription());
                bot.execute(new SendMessage(chatId, text));
            });
            responseObserver.onNext(SendUpdateResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("SendUpdate error");
            responseObserver.onError(io.grpc.Status.INTERNAL
                .withDescription("Internal error").asRuntimeException());
        } finally {
            MDC.clear();
        }
    }
}
