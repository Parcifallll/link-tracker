package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.Link;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LinkRepository {
    Link save(long chatId, Link link);

    void delete(long chatId, URI url);

    List<Link> findAll(long chatId);

    Optional<Link> findByUrl(long chatId, URI url);

    Map<Long, List<Link>> findAllWithChatIds();
}
