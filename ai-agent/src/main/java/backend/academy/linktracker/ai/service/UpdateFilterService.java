package backend.academy.linktracker.ai.service;

import backend.academy.linktracker.ai.dto.RawLinkUpdate;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateFilterService {

    private final AiAgentProperties properties;

    public boolean passes(RawLinkUpdate update) {
        var f = properties.filtering();

        if (update.description() == null) {
            log.debug("Update {} discarded: null description", update.id());
            return false;
        }
        if (update.author() != null
                && f.excludedAuthors().stream().anyMatch(a -> a.equalsIgnoreCase(update.author()))) {
            log.debug("Update {} discarded: excluded author '{}'", update.id(), update.author());
            return false;
        }
        if (update.description().length() < f.minLength()) {
            log.debug(
                    "Update {} discarded: too short ({} < {})",
                    update.id(),
                    update.description().length(),
                    f.minLength());
            return false;
        }
        String lower = update.description().toLowerCase();
        if (f.stopWords().stream().anyMatch(w -> lower.contains(w.toLowerCase()))) {
            log.debug("Update {} discarded: contains stop-word", update.id());
            return false;
        }
        return true;
    }
}
