package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.UserSession;

public interface UserSessionRepository {
    UserSession findById(long chatId);

    void save(long chatId, UserSession session);
}
