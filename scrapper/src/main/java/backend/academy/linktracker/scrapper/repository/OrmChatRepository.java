package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.Chat;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
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
