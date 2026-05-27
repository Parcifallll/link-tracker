package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.cache")
@Validated
@Getter
@Setter
public class CacheProperties {

    public static final String LINKS_CACHE = "links";

    @NotNull
    private Duration ttl;
}
