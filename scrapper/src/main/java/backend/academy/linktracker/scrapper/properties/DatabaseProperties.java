package backend.academy.linktracker.scrapper.properties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.database")
@Validated
@Getter
@Setter
@NoArgsConstructor
public class DatabaseProperties {
    private String accessType = "SQL";
}
