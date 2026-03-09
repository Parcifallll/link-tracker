package backend.academy.linktracker.bot.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.util.List;

@Getter
@Setter
public class UserSession {
    private Long chatId;
    private UserState state = UserState.IDLE;
    private URI pendingLink;
    private List<String> tags;
    private List<String> filters;
}
