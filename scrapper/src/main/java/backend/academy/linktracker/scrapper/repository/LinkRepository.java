package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.dto.link.LinkWithChats;
import backend.academy.linktracker.scrapper.model.Link;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LinkRepository {
    Link save(long chatId, Link link);

    void delete(long chatId, URI url);

    List<Link> findAll(long chatId);

    Optional<Link> findByUrl(long chatId, URI url);

    void updateLastCheckedAt(long linkId, Instant lastCheckedAt);

    List<LinkWithChats> findLinksToCheck(int limit);
}
