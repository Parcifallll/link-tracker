package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.properties.CacheProperties;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final ChatRepository chatRepository;

    // Cache key = chatId (matches Tg-Chat-Id header value)
    @CacheEvict(value = CacheProperties.LINKS_CACHE, key = "#chatId")
    public Link add(long chatId, URI url, List<String> tags, List<String> filters) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        if (linkRepository.findByUrl(chatId, url).isPresent()) {
            throw new LinkAlreadyExistsException(url);
        }
        return linkRepository.save(chatId, new Link(0, url, tags, filters));
    }

    // Cache key = chatId (matches Tg-Chat-Id header value)
    @CacheEvict(value = CacheProperties.LINKS_CACHE, key = "#chatId")
    public Link remove(long chatId, URI url) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        Link link = linkRepository.findByUrl(chatId, url).orElseThrow(() -> new LinkNotFoundException(url));
        linkRepository.delete(chatId, url);
        return link;
    }

    // Cache key = chatId (matches Tg-Chat-Id header value)
    @Cacheable(value = CacheProperties.LINKS_CACHE, key = "#chatId")
    public List<Link> findAll(long chatId) {
        if (!chatRepository.exists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        return linkRepository.findAll(chatId);
    }
}
