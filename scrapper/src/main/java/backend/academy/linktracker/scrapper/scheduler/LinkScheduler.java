// scrapper/src/main/java/backend/academy/linktracker/scrapper/scheduler/LinkScheduler.java
package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkUpdateService;
import backend.academy.linktracker.scrapper.service.MessageSender;
import backend.academy.linktracker.scrapper.service.UpdateInfo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        log.info("Starting link updates check (batch size: {})", schedulerProperties.getBatchSize());

        Map<Link, List<Long>> linksWithChatIds = linkRepository.findLinksToCheck(schedulerProperties.getBatchSize());

        if (linksWithChatIds.isEmpty()) {
            log.info("No links to check");
            return;
        }

        log.info("Checking {} links", linksWithChatIds.size());

        linksWithChatIds.forEach((link, chatIds) -> {
            MDC.put("url", link.getUrl().toString());
            MDC.put("linkId", String.valueOf(link.getId()));

            try {
                Optional<UpdateInfo> updateOpt = linkUpdateService.checkUpdate(link);

                if (updateOpt.isPresent()) {
                    UpdateInfo updateInfo = updateOpt.orElseThrow();

                    linkRepository.updateLastCheckedAt(link.getId(), link.getLastCheckedAt());

                    messageSender.sendUpdate(link, updateInfo, chatIds);

                    log.info("Updates found and sent for link: {} | title: {}", link.getUrl(), updateInfo.linkTitle());
                } else {
                    log.debug("No updates for link: {}", link.getUrl());
                }

            } catch (Exception e) {
                log.error("Error checking link: {}", link.getUrl(), e);
                messageSender.sendError(link, e.getMessage(), chatIds);
            } finally {
                MDC.clear();
            }
        });

        log.info("Link updates check completed");
    }
}
