package backend.academy.linktracker.scrapper.client.stackoverflow;

import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "https://api.stackexchange.com/2.3")
public interface StackOverflowClient {

    @GetExchange("/questions/{id}")
    StackOverflowResponse getQuestion(
        @PathVariable long id,
        @RequestParam String site,
        @RequestParam String key,
        @RequestParam("access_token") String accessToken
    );
}
