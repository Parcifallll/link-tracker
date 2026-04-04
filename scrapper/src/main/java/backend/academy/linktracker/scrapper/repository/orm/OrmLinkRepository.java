package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.Chat;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.orm.entity.LinkEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.orm.entity.SubscriptionId;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "ORM")
public class OrmLinkRepository implements LinkRepository {

    private final EntityManager em;

    @Override
    @Transactional
    public Link save(long chatId, Link link) {
        LinkEntity linkEntity = findLinkByUrl(link.getUrl().toString())
            .orElseGet(() -> {
                LinkEntity e = new LinkEntity(link.getUrl().toString());
                em.persist(e);
                return e;
            });

        Chat chat = em.find(Chat.class, chatId);
        String[] tags = toArray(link.getTags());
        String[] filters = toArray(link.getFilters());

        SubscriptionEntity sub = em.find(SubscriptionEntity.class,
            new SubscriptionId(chatId, linkEntity.getId()));
        if (sub == null) {
            sub = new SubscriptionEntity(chat, linkEntity, tags, filters);
            em.persist(sub);
        } else {
            sub.setTags(tags);
            sub.setFilters(filters);
        }

        return toLink(linkEntity, tags, filters);
    }

    @Override
    @Transactional
    public void delete(long chatId, URI url) {
        findSubscriptionByChatIdAndUrl(chatId, url.toString())
            .ifPresent(em::remove);
    }

    @Override
    public List<Link> findAll(long chatId) {
        return em.createQuery("""
            SELECT s FROM SubscriptionEntity s
            JOIN FETCH s.link
            WHERE s.chat.chatId = :chatId
            """, SubscriptionEntity.class)
            .setParameter("chatId", chatId)
            .getResultList()
            .stream()
            .map(s -> toLink(s.getLink(), s.getTags(), s.getFilters()))
            .toList();
    }

    @Override
    public Optional<Link> findByUrl(long chatId, URI url) {
        return findSubscriptionByChatIdAndUrl(chatId, url.toString())
            .map(s -> toLink(s.getLink(), s.getTags(), s.getFilters()));
    }

    @Override
    public Map<Long, List<Link>> findAllWithChatIds() {
        Map<Long, List<Link>> result = new HashMap<>();
        em.createQuery("""
            SELECT s FROM SubscriptionEntity s
            JOIN FETCH s.link
            JOIN FETCH s.chat
            """, SubscriptionEntity.class)
            .getResultList()
            .forEach(s -> {
                long cid = s.getChat().getChatId();
                result.computeIfAbsent(cid, id -> new ArrayList<>())
                    .add(toLink(s.getLink(), s.getTags(), s.getFilters()));
            });
        return result;
    }

    private Optional<LinkEntity> findLinkByUrl(String url) {
        List<LinkEntity> result = em.createQuery(
                "SELECT l FROM LinkEntity l WHERE l.url = :url", LinkEntity.class)
            .setParameter("url", url)
            .getResultList();
        return result.stream().findFirst();
    }

    private Optional<SubscriptionEntity> findSubscriptionByChatIdAndUrl(long chatId, String url) {
        List<SubscriptionEntity> result = em.createQuery("""
            SELECT s FROM SubscriptionEntity s
            JOIN FETCH s.link
            WHERE s.chat.chatId = :chatId AND s.link.url = :url
            """, SubscriptionEntity.class)
            .setParameter("chatId", chatId)
            .setParameter("url", url)
            .getResultList();
        return result.stream().findFirst();
    }

    private Link toLink(LinkEntity entity, String[] tags, String[] filters) {
        Link link = new Link(
            entity.getId(),
            URI.create(entity.getUrl()),
            tags == null ? List.of() : Arrays.asList(tags),
            filters == null ? List.of() : Arrays.asList(filters)
        );
        link.setLastCheckedAt(entity.getLastCheckedAt());
        return link;
    }

    private String[] toArray(List<String> list) {
        return list == null ? new String[0] : list.toArray(String[]::new);
    }

    @Override
    @Transactional
    public void updateLastCheckedAt(long linkId, Instant lastCheckedAt) {
        LinkEntity entity = em.find(LinkEntity.class, linkId);
        if (entity != null) entity.setLastCheckedAt(lastCheckedAt);
    }
}
