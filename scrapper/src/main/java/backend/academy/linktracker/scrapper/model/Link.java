package backend.academy.linktracker.scrapper.model;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Link {
    private final long id;
    private final URI url;
    private final List<String> tags;
    private final List<String> filters;
    private Instant lastCheckedAt;

    public Link(long id, URI url, List<String> tags, List<String> filters) {
        this.id = id;
        this.url = url;
        this.tags = tags;
        this.filters = filters;
        this.lastCheckedAt = Instant.now();
    }
}
