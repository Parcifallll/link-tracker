package backend.academy.linktracker.scrapper.service.updates;

import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateInfo;
import java.time.Instant;
import java.util.Optional;

public interface LinkUpdateChecker {

    boolean supports(Link link);

    Optional<UpdateInfo> check(Link link);

    default String truncate(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }

    default void updateLastCheckedTime(Link link) {
        if (link != null) {
            link.setLastCheckedAt(Instant.now());
        }
    }
}
