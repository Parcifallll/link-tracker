package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.entity.SubscriptionEntity;
import backend.academy.linktracker.scrapper.repository.entity.SubscriptionId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionEntityJpaRepository extends JpaRepository<SubscriptionEntity, SubscriptionId> {

    @Query("SELECT s FROM SubscriptionEntity s JOIN FETCH s.link WHERE s.chat.chatId = :chatId")
    List<SubscriptionEntity> findByChatId(@Param("chatId") long chatId);

    @Query("""
        SELECT s FROM SubscriptionEntity s
        JOIN FETCH s.link
        WHERE s.chat.chatId = :chatId AND s.link.url = :url
        """)
    Optional<SubscriptionEntity> findByChatIdAndUrl(@Param("chatId") long chatId, @Param("url") String url);

    @Query("SELECT s FROM SubscriptionEntity s JOIN FETCH s.chat WHERE s.link.id IN :linkIds")
    List<SubscriptionEntity> findByLinkIds(@Param("linkIds") List<Long> linkIds);
}
