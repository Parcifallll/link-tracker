package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.model.Chat;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "ORM")
public class OrmChatRepository implements ChatRepository {

    private final EntityManager em;

    @Override
    @Transactional
    public void save(Chat chat) {
        em.merge(chat);
    }

    @Override
    @Transactional
    public void delete(long chatId) {
        Chat chat = em.find(Chat.class, chatId);
        if (chat != null) em.remove(chat);
    }

    @Override
    public boolean exists(long chatId) {
        return em.find(Chat.class, chatId) != null;
    }
}
