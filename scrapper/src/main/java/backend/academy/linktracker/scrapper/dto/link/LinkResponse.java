package backend.academy.linktracker.scrapper.dto.link;

import java.net.URI;
import java.util.List;

public record LinkResponse(
    long id,
    URI url,
    List<String> tags,
    List<String> filters
) {}
