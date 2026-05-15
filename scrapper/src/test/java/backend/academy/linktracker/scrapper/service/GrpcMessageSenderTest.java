package backend.academy.linktracker.scrapper.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.grpc.BotUpdateServiceGrpc;
import backend.academy.linktracker.grpc.SendUpdateRequest;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.service.senders.GrpcMessageSender;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateInfo;
import io.grpc.ManagedChannel;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GrpcMessageSenderTest {

    @Mock
    private GrpcChannelFactory channelFactory;

    @Mock
    private ManagedChannel managedChannel;

    @Mock
    private BotUpdateServiceGrpc.BotUpdateServiceBlockingStub stub;

    private GrpcMessageSender grpcMessageSender;

    @BeforeEach
    void setUp() {
        when(channelFactory.createChannel("bot")).thenReturn(managedChannel);
        grpcMessageSender = new GrpcMessageSender(channelFactory);
        ReflectionTestUtils.setField(grpcMessageSender, "stub", stub);
    }

    @Test
    void testSendUpdate() {
        // Arrange
        Link link = createLink("https://github.com/user/repo", 1L);
        List<Long> chatIds = List.of(123L, 456L);
        UpdateInfo updateInfo = new UpdateInfo("New Release", Map.of());

        // Act
        grpcMessageSender.sendUpdate(link, updateInfo, chatIds);

        // Assert
        verify(stub).sendUpdate(any(SendUpdateRequest.class));
    }

    @Test
    void testSendError() {
        // Arrange
        Link link = createLink("https://stackoverflow.com/questions/123", 2L);
        List<Long> chatIds = List.of(789L);
        String errorMessage = "Connection timeout";

        // Act
        grpcMessageSender.sendError(link, errorMessage, chatIds);

        // Assert
        verify(stub).sendUpdate(any(SendUpdateRequest.class));
    }

    @Test
    void testSendUpdateWithMultipleChatIds() {
        // Arrange
        Link link = createLink("https://github.com/user/repo", 1L);
        List<Long> chatIds = List.of(111L, 222L, 333L);
        UpdateInfo updateInfo = new UpdateInfo("Update", Map.of());

        // Act
        grpcMessageSender.sendUpdate(link, updateInfo, chatIds);

        // Assert
        verify(stub).sendUpdate(any(SendUpdateRequest.class));
    }

    private Link createLink(String url, Long id) {
        return new Link(id, URI.create(url), List.of(), List.of());
    }
}
