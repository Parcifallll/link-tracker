package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.Chat;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryChatRepository implements ChatRepository {

    private final Set<Long> chats = ConcurrentHashMap.newKeySet();

    @Override
    public void save(Chat chat) {
        chats.add(chat.chatId());
    }

    @Override
    public void delete(long chatId) {
        chats.remove(chatId);
    }

    @Override
    public boolean exists(long chatId) {
        return chats.contains(chatId);
    }
}
