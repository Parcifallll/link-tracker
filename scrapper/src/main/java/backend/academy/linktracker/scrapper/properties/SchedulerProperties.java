// scrapper/src/main/java/backend/academy/linktracker/scrapper/properties/SchedulerProperties.java
package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.Min;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.scheduler")
@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class SchedulerProperties {

    private Duration interval;

    @Min(1)
    private int batchSize;
}
