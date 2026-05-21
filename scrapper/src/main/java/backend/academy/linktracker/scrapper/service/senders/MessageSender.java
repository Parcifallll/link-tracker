package backend.academy.linktracker.scrapper.service.senders;

import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.service.updates.dto.UpdateInfo;
import java.util.List;

public interface MessageSender {
    void sendUpdate(Link link, UpdateInfo updateInfo, List<Long> chatIds);

    void sendError(Link link, String errorMessage, List<Long> chatIds);
}
