package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.grpc.BotUpdateGrpcClient;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkUpdateService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class LinkScheduler {

    private final LinkRepository linkRepository;
    private final LinkUpdateService linkUpdateService;
    private final BotUpdateGrpcClient botUpdateGrpcClient;

    @Scheduled(fixedDelayString = "${app.scheduler.interval}")
    public void checkUpdates() {
        log.atInfo().log("Checking link updates");

        Map<Long, List<Link>> linksByChatId = linkRepository.findAllWithChatIds();

        linksByChatId.forEach((chatId, links) -> links.forEach(link -> {
            MDC.put("chatId", String.valueOf(chatId));
            MDC.put("url", link.getUrl().toString());
            try {
                if (linkUpdateService.hasUpdate(link)) {
                    linkRepository.updateLastCheckedAt(link.getId(), link.getLastCheckedAt());
                    botUpdateGrpcClient.sendUpdate(link, List.of(chatId));
                }
            } finally {
                MDC.clear();
            }
        }));
    }
}
