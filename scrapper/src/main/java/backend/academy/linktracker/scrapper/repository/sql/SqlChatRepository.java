package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.Chat;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "SQL", matchIfMissing = true)
public class SqlChatRepository implements ChatRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void save(Chat chat) {
        jdbcTemplate.update("INSERT INTO chats (chat_id) VALUES (?) ON CONFLICT DO NOTHING", chat.chatId());
    }

    @Override
    public void delete(long chatId) {
        jdbcTemplate.update("DELETE FROM chats WHERE chat_id = ?", chatId);
    }

    @Override
    public boolean exists(long chatId) {
        Integer count =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM chats WHERE chat_id = ?", Integer.class, chatId);
        return count != null && count > 0;
    }
}
