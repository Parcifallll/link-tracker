package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.Link;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkUpdateService {

    private final List<LinkUpdateChecker> checkers;

    public Optional<UpdateInfo> checkUpdate(Link link) {
        if (link == null || link.getUrl() == null) {
            return Optional.empty();
        }

        return checkers.stream()
                .filter(checker -> checker.supports(link))
                .findFirst()
                .flatMap(checker -> checker.check(link));
    }
}
