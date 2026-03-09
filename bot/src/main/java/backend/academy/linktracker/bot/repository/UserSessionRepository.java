package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.UserSession;
import backend.academy.linktracker.bot.model.UserState;
import java.net.URI;
import java.util.List;

public interface UserSessionRepository {
    UserSession findById(long chatId);
    void save(long chatId, UserSession session);
}
