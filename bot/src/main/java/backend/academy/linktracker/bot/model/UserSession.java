package backend.academy.linktracker.bot.model;

import java.net.URI;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSession {
    private Long chatId;
    private UserState state = UserState.IDLE;
    private URI pendingLink;
    private List<String> tags;
    private List<String> filters;
}
