package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.User;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserRepository implements UserRepository {

    // will be replaced by DB-storage
    private final Set<Long> users = ConcurrentHashMap.newKeySet();

    @Override
    public boolean exists(long chatId) {
        return users.contains(chatId);
    }

    @Override
    public void save(User user) {
        users.add(user.chatId());
    }
}
