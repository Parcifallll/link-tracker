package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.dto.link.LinkWithChats;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.senders.MessageSender;
import backend.academy.linktracker.scrapper.service.updates.LinkUpdateService;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateInfo;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final MessageSender messageSender;
    private final SchedulerProperties schedulerProperties;

    @Scheduled(fixedDelayString = "${app.scheduler.interval}")
    public void checkUpdates() {
        log.debug("Starting link updates check (batch size: {})", schedulerProperties.getBatchSize());

        List<LinkWithChats> linksToCheck = linkRepository.findLinksToCheck(schedulerProperties.getBatchSize());

        if (linksToCheck.isEmpty()) {
            log.debug("No links to check");
            return;
        }

        AtomicInteger updatedCount = new AtomicInteger(0);

        linksToCheck.forEach(linkWithChats -> {
            var link = linkWithChats.link();
            var chatIds = linkWithChats.chatIds();

            MDC.put("url", link.getUrl().toString());
            MDC.put("linkId", String.valueOf(link.getId()));

            try {
                UpdateInfo updateInfo = linkUpdateService.checkUpdate(link).orElse(null);
                linkRepository.updateLastCheckedAt(link.getId(), link.getLastCheckedAt());

                if (updateInfo == null) {
                    log.debug("No updates for link: {}", link.getUrl());
                    return;
                }

                messageSender.sendUpdate(link, updateInfo, chatIds);
                updatedCount.incrementAndGet();

                log.info("Updates found and sent for link: {} | title: {}", link.getUrl(), updateInfo.linkTitle());

            } catch (Exception e) {
                log.error("Error checking link: {}", link.getUrl(), e);
                messageSender.sendError(link, e.getMessage(), chatIds);
            } finally {
                MDC.clear();
            }
        });

        log.info("Link updates check completed. Checked: {}, Updated: {}", linksToCheck.size(), updatedCount.get());
    }
}
