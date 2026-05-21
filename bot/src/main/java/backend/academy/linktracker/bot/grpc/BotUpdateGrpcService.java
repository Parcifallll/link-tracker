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
            log.atInfo().log("Received update via gRPC from scrapper");

            String formattedMessage = formatMessage(request);

            request.getTgChatIdsList().forEach(chatId -> {
                MDC.put("chatId", String.valueOf(chatId));
                bot.execute(new SendMessage(chatId, formattedMessage));
                log.debug("Message sent to chat {}", chatId);
            });

            responseObserver.onNext(
                    SendUpdateResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.atError().addKeyValue("error", e.getMessage()).log("Failed to process update");
            responseObserver.onError(
                    io.grpc.Status.INTERNAL.withDescription("Internal error").asRuntimeException());
        } finally {
            MDC.clear();
        }
    }

    private String formatMessage(SendUpdateRequest request) {
        if (!request.getError().isBlank()) {
            return "Ошибка при проверке ссылки\n\n" + "Ссылка: "
                    + request.getUrl() + "\n" + "Ошибка: "
                    + request.getError();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("**Обновление по ссылке**\n\n");
        sb.append("Ссылка: ").append(request.getUrl()).append("\n");
        sb.append("Название: ").append(request.getTitle()).append("\n\n");

        request.getUpdatesList().forEach(item -> {
            sb.append("**").append(item.getType()).append("**\n");
            sb.append("• ").append(item.getTitle()).append("\n");

            if (!item.getAuthor().isBlank()) {
                sb.append("Автор: ").append(item.getAuthor()).append("\n");
            }
            sb.append("Дата: ").append(item.getCreatedAt()).append("\n");

            if (!item.getPreview().isBlank()) {
                sb.append("Текст: ").append(item.getPreview()).append("\n");
            }
            sb.append("\n");
        });

        return sb.toString();
    }
}
