package backend.academy.linktracker.scrapper.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record UpdateInfo(
    String linkTitle,
    Map<UpdateType, List<UpdateItem>> itemsByType
) {
    public record UpdateItem(
        String title,
        String author,
        Instant createdAt,
        String preview
    ) {}
}
