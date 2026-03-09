package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.UserSession;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserSessionRepository implements UserSessionRepository {
    private ConcurrentHashMap<Long, UserSession> sessions = new ConcurrentHashMap<>();

    @Override
    public UserSession findById(long chatId) {
        return sessions.computeIfAbsent(chatId, id -> new UserSession());
    }

    @Override
    public void save(long chatId, UserSession session) {
        sessions.put(chatId, session);
    }
}
