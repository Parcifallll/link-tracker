package backend.academy.linktracker.scrapper.dto.link;

import java.util.List;

public record ListLinksResponse(List<LinkResponse> links, int size) {}
