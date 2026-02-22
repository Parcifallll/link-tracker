package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.User;

public interface UserRepository {

    boolean exists(long chatId);

    void save(User user);
}
