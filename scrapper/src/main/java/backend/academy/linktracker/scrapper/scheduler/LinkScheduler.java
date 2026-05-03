package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.dto.link.LinkWithChats;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkUpdateService;
import backend.academy.linktracker.scrapper.service.MessageSender;
import backend.academy.linktracker.scrapper.service.UpdateInfo;
import java.util.List;
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

        List<LinkWithChats> linksToCheck = linkRepository.findLinksToCheck(schedulerProperties.getBatchSize());

        if (linksToCheck.isEmpty()) {
            log.info("No links to check");
            return;
        }

        log.info("Checking {} links", linksToCheck.size());

        linksToCheck.forEach(linkWithChats -> {
            var link = linkWithChats.link();
            var chatIds = linkWithChats.chatIds();

            MDC.put("url", link.getUrl().toString());
            MDC.put("linkId", String.valueOf(link.getId()));

            try {
                Optional<UpdateInfo> updateOpt = linkUpdateService.checkUpdate(link);

                if (updateOpt.isEmpty()) {
                    log.debug("No updates for link: {}", link.getUrl());
                    return;
                }

                UpdateInfo updateInfo = updateOpt.orElseThrow(() -> new RuntimeException("UpdateInfo is null"));

                linkRepository.updateLastCheckedAt(link.getId(), link.getLastCheckedAt());

                messageSender.sendUpdate(link, updateInfo, chatIds);

                log.info("Updates found and sent for link: {} | title: {}", link.getUrl(), updateInfo.linkTitle());
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
