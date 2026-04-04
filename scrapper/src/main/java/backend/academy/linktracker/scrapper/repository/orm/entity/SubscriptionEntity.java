package backend.academy.linktracker.scrapper.repository.orm.entity;

import backend.academy.linktracker.scrapper.model.Chat;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionEntity {
    @EmbeddedId
    private SubscriptionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatId")
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("linkId")
    @JoinColumn(name = "link_id")
    private LinkEntity link;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags = new String[0];

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "filters", columnDefinition = "text[]")
    private String[] filters = new String[0];

    public SubscriptionEntity(Chat chat, LinkEntity link, String[] tags, String[] filters) {
        this.id = new SubscriptionId(chat.getChatId(), link.getId());
        this.chat = chat;
        this.link = link;
        this.tags = tags;
        this.filters = filters;
    }
}
