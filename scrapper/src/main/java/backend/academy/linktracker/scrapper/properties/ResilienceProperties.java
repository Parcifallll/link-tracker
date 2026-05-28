package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.resilience")
@Validated
@Getter
@Setter
public class ResilienceProperties {

    @NotNull
    private Timeout timeout;

    @NotNull
    private Retry retry;

    @NotNull
    private CircuitBreaker circuitBreaker;

    @NotNull
    private RateLimiter rateLimiter;

    @Getter
    @Setter
    public static class Timeout {
        @NotNull
        private Duration connect;

        @NotNull
        private Duration read;
    }

    @Getter
    @Setter
    public static class Retry {
        @Min(1)
        private int maxAttempts;

        @NotNull
        private Duration waitDuration;

        @NotEmpty
        private List<Integer> retryableStatuses;
    }

    @Getter
    @Setter
    public static class CircuitBreaker {
        @Min(1)
        private int slidingWindowSize;

        @Min(1)
        private int minimumRequiredCalls;

        private float failureRateThreshold;

        @Min(1)
        private int permittedCallsInHalfOpenState;

        @NotNull
        private Duration waitDurationInOpenState;
    }

    @Getter
    @Setter
    public static class RateLimiter {
        @Min(1)
        private int limitForPeriod;

        @NotNull
        private Duration limitRefreshPeriod;

        @NotNull
        private Duration timeoutDuration;
    }
}
