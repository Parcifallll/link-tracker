package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.ResilienceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpStatusCodeException;

@Configuration
@RequiredArgsConstructor
public class ResilienceConfiguration {

    public static final String GITHUB = "github";
    public static final String STACKOVERFLOW = "stackoverflow";

    private final ResilienceProperties props;

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        ResilienceProperties.CircuitBreaker cb = props.getCircuitBreaker();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(cb.getSlidingWindowSize())
                .minimumNumberOfCalls(cb.getMinimumRequiredCalls())
                .failureRateThreshold(cb.getFailureRateThreshold())
                .waitDurationInOpenState(cb.getWaitDurationInOpenState())
                .permittedNumberOfCallsInHalfOpenState(cb.getPermittedCallsInHalfOpenState())
                .build();

        return CircuitBreakerRegistry.of(Map.of(
                GITHUB, config,
                STACKOVERFLOW, config));
    }

    @Bean
    public RetryRegistry retryRegistry() {
        ResilienceProperties.Retry r = props.getRetry();
        Set<Integer> retryableStatuses = Set.copyOf(r.getRetryableStatuses());

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(r.getMaxAttempts())
                .waitDuration(r.getWaitDuration())
                .retryOnException(ex -> {
                    if (ex instanceof HttpStatusCodeException httpEx) {
                        return retryableStatuses.contains(httpEx.getStatusCode().value());
                    }
                    return true;
                })
                .build();

        return RetryRegistry.of(Map.of(
                GITHUB, config,
                STACKOVERFLOW, config));
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        ResilienceProperties.RateLimiter rl = props.getRateLimiter();

        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(rl.getLimitForPeriod())
                .limitRefreshPeriod(rl.getLimitRefreshPeriod())
                .timeoutDuration(rl.getTimeoutDuration())
                .build();

        return RateLimiterRegistry.of(Map.of(GITHUB, config));
    }
}
