package backend.academy.linktracker.scrapper.client.bot;

import backend.academy.linktracker.scrapper.client.bot.dto.LinkUpdate;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface BotClient {

    @PostExchange("/updates")
    void sendUpdate(LinkUpdate update);
}
