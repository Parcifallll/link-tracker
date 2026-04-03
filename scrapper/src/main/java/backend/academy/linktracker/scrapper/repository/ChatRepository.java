package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.Chat;

public interface ChatRepository {
    void save(Chat chat);

    void delete(long chatId);

    boolean exists(long chatId);
}
