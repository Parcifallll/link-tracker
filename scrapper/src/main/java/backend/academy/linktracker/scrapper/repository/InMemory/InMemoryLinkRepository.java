package backend.academy.linktracker.scrapper.repository.InMemory;

import backend.academy.linktracker.scrapper.model.Link;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import backend.academy.linktracker.scrapper.repository.LinkRepository;

public class InMemoryLinkRepository implements LinkRepository {

    // chatId -> list of links
    private final Map<Long, List<Link>> links = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public Link save(long chatId, Link link) {
        Link saved = new Link(idSequence.getAndIncrement(), link.getUrl(), link.getTags(), link.getFilters());
        links.computeIfAbsent(chatId, id -> new ArrayList<>()).add(saved);
        return saved;
    }

    @Override
    public void delete(long chatId, URI url) {
        List<Link> chatLinks = links.getOrDefault(chatId, List.of());
        chatLinks.removeIf(l -> l.getUrl().equals(url));
    }

    @Override
    public List<Link> findAll(long chatId) {
        return List.copyOf(links.getOrDefault(chatId, List.of()));
    }

    @Override
    public Optional<Link> findByUrl(long chatId, URI url) {
        return links.getOrDefault(chatId, List.of()).stream()
                .filter(l -> l.getUrl().equals(url))
                .findFirst();
    }

    @Override
    public Map<Long, List<Link>> findAllWithChatIds() {
        return Map.copyOf(links);
    }
}
