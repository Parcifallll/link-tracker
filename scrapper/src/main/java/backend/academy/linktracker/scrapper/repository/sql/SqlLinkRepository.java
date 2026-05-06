package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.dto.link.LinkWithChats;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "SQL", matchIfMissing = true)
public class SqlLinkRepository implements LinkRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public Link save(long chatId, Link link) {
        jdbcTemplate.update("""
            INSERT INTO links (url, last_checked_at)
            VALUES (?, ?)
            ON CONFLICT (url) DO UPDATE SET last_checked_at = EXCLUDED.last_checked_at
            """, link.getUrl().toString(), Timestamp.from(link.getLastCheckedAt()));

        Long linkId = jdbcTemplate.queryForObject(
                "SELECT id FROM links WHERE url = ?", Long.class, link.getUrl().toString());

        // insert subscription with tags and filters
        Array tags = createArray(link.getTags());
        Array filters = createArray(link.getFilters());
        jdbcTemplate.update("""
            INSERT INTO subscriptions (chat_id, link_id, tags, filters)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (chat_id, link_id) DO UPDATE SET tags = EXCLUDED.tags, filters = EXCLUDED.filters
            """, chatId, linkId, tags, filters);

        return new Link(linkId, link.getUrl(), link.getTags(), link.getFilters());
    }

    @Override
    @Transactional
    public void delete(long chatId, URI url) {
        jdbcTemplate.update("""
            DELETE FROM subscriptions
            WHERE chat_id = ? AND link_id = (SELECT id FROM links WHERE url = ?)
            """, chatId, url.toString());
    }

    @Override
    public List<Link> findAll(long chatId) {
        return jdbcTemplate.query("""
            SELECT l.id, l.url, l.last_checked_at, s.tags, s.filters
            FROM links l
            JOIN subscriptions s ON l.id = s.link_id
            WHERE s.chat_id = ?
            """, (rs, rowNum) -> mapLink(rs), chatId);
    }

    @Override
    public Optional<Link> findByUrl(long chatId, URI url) {
        List<Link> result = jdbcTemplate.query("""
            SELECT l.id, l.url, l.last_checked_at, s.tags, s.filters
            FROM links l
            JOIN subscriptions s ON l.id = s.link_id
            WHERE s.chat_id = ? AND l.url = ?
            """, (rs, rowNum) -> mapLink(rs), chatId, url.toString());
        return result.stream().findFirst();
    }

    private Link mapLink(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        URI url = URI.create(rs.getString("url"));
        Instant lastCheckedAt = rs.getTimestamp("last_checked_at").toInstant();
        List<String> tags = arrayToList(rs.getArray("tags"));
        List<String> filters = arrayToList(rs.getArray("filters"));
        Link link = new Link(id, url, tags, filters);
        link.setLastCheckedAt(lastCheckedAt);
        return link;
    }

    private List<String> arrayToList(Array array) throws SQLException {
        if (array == null) return List.of();
        return List.of((String[]) array.getArray());
    }

    private Array createArray(List<String> items) {
        return jdbcTemplate.execute((java.sql.Connection con) ->
                con.createArrayOf("text", items == null ? new String[0] : items.toArray(String[]::new)));
    }

    @Override
    public void updateLastCheckedAt(long linkId, Instant lastCheckedAt) {
        jdbcTemplate.update("UPDATE links SET last_checked_at = ? WHERE id = ?", Timestamp.from(lastCheckedAt), linkId);
    }

    @Override
    public List<LinkWithChats> findLinksToCheck(int limit) {
        // mock - no further support for sql implementation (not required)
        return List.of();
    }
}
