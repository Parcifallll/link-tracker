package backend.academy.linktracker.scrapper.client.stackoverflow;

import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowAnswersResponse;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowCommentsResponse;
import backend.academy.linktracker.scrapper.client.stackoverflow.dto.StackOverflowResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "https://api.stackexchange.com/2.3")
public interface StackOverflowClient {

    @GetExchange("/questions/{id}")
    StackOverflowResponse getQuestion(@PathVariable long id, @RequestParam String site, @RequestParam String key);

    @GetExchange("/questions/{id}/answers?fromdate={fromdate}&order=desc&sort=creation&site={site}&key={key}")
    StackOverflowAnswersResponse getAnswers(
            @PathVariable long id, @PathVariable long fromdate, @PathVariable String site, @PathVariable String key);

    @GetExchange("/questions/{id}/comments?fromdate={fromdate}&order=desc&sort=creation&site={site}&key={key}")
    StackOverflowCommentsResponse getComments(
            @PathVariable long id, @PathVariable long fromdate, @PathVariable String site, @PathVariable String key);
}
