package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.Chat;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.dto.link.LinkWithChats;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.repository.entity.LinkEntity;
import backend.academy.linktracker.scrapper.repository.entity.SubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.entity.SubscriptionId;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "ORM")
@RequiredArgsConstructor
public class OrmLinkRepository implements LinkRepository {

    private final LinkEntityJpaRepository linkRepo;
    private final SubscriptionEntityJpaRepository subscriptionRepo;
    private final ChatJpaRepository chatRepo;

    @Override
    @Transactional
    public Link save(long chatId, Link link) {
        LinkEntity linkEntity = linkRepo.findByUrl(link.getUrl().toString())
            .orElseGet(() -> linkRepo.save(new LinkEntity(link.getUrl().toString())));

        Chat chat = chatRepo.getReferenceById(chatId);
        String[] tags = toArray(link.getTags());
        String[] filters = toArray(link.getFilters());

        SubscriptionId subId = new SubscriptionId(chatId, linkEntity.getId());
        SubscriptionEntity sub = subscriptionRepo.findById(subId)
            .orElseGet(() -> new SubscriptionEntity(chat, linkEntity, tags, filters));

        sub.setTags(tags);
        sub.setFilters(filters);
        subscriptionRepo.save(sub);

        return toLink(linkEntity, tags, filters);
    }

    @Override
    @Transactional
    public void delete(long chatId, URI url) {
        subscriptionRepo.findByChatIdAndUrl(chatId, url.toString())
            .ifPresent(subscriptionRepo::delete);
    }

    @Override
    public List<Link> findAll(long chatId) {
        return subscriptionRepo.findByChatId(chatId).stream()
            .map(s -> toLink(s.getLink(), s.getTags(), s.getFilters()))
            .toList();
    }

    @Override
    public Optional<Link> findByUrl(long chatId, URI url) {
        return subscriptionRepo.findByChatIdAndUrl(chatId, url.toString())
            .map(s -> toLink(s.getLink(), s.getTags(), s.getFilters()));
    }

    @Override
    @Transactional
    public void updateLastCheckedAt(long linkId, Instant lastCheckedAt) {
        linkRepo.findById(linkId).ifPresent(entity -> {
            entity.setLastCheckedAt(lastCheckedAt);
            linkRepo.save(entity);
        });
    }

    @Override
    public List<LinkWithChats> findLinksToCheck(int limit) {
        List<LinkEntity> linkEntities =
            linkRepo.findTopByOrderByLastCheckedAtAsc(PageRequest.of(0, limit));

        if (linkEntities.isEmpty()) {
            return List.of();
        }

        List<Long> linkIds = linkEntities.stream().map(LinkEntity::getId).toList();

        Map<Long, List<Long>> chatIdsByLinkId = new HashMap<>();
        subscriptionRepo.findByLinkIds(linkIds).forEach(sub -> {
            long linkId = sub.getLink().getId();
            chatIdsByLinkId
                .computeIfAbsent(linkId, k -> new ArrayList<>())
                .add(sub.getChat().getChatId());
        });

        return linkEntities.stream()
            .map(entity -> new LinkWithChats(
                toLink(entity, new String[0], new String[0]),
                chatIdsByLinkId.getOrDefault(entity.getId(), List.of())))
            .toList();
    }

    private Link toLink(LinkEntity entity, String[] tags, String[] filters) {
        Link link = new Link(
            entity.getId(),
            URI.create(entity.getUrl()),
            tags == null ? List.of() : Arrays.asList(tags),
            filters == null ? List.of() : Arrays.asList(filters));
        link.setLastCheckedAt(entity.getLastCheckedAt());
        return link;
    }

    private String[] toArray(List<String> list) {
        return list == null ? new String[0] : list.toArray(String[]::new);
    }
}
