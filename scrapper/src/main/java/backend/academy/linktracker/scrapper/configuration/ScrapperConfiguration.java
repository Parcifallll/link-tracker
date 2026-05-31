package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.client.github.GithubClient;
import backend.academy.linktracker.scrapper.client.stackoverflow.StackOverflowClient;
import backend.academy.linktracker.scrapper.properties.GithubProperties;
import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@RequiredArgsConstructor
public class ScrapperConfiguration {

    private final ResilienceProperties resilienceProperties;

    @Bean
    public GithubClient githubClient(GithubProperties properties) {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + properties.getToken())
                .defaultHeader("Accept", "application/vnd.github+json")
                .requestFactory(buildRequestFactory())
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(GithubClient.class);
    }

    @Bean
    public StackOverflowClient stackOverflowClient() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.stackexchange.com/2.3")
                .requestFactory(buildRequestFactory())
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(StackOverflowClient.class);
    }

    private SimpleClientHttpRequestFactory buildRequestFactory() {
        ResilienceProperties.Timeout timeout = resilienceProperties.getTimeout();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) timeout.getConnect().toMillis());
        factory.setReadTimeout((int) timeout.getRead().toMillis());
        return factory;
    }
}
