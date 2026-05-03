package backend.academy.linktracker.scrapper.dto.link;

import backend.academy.linktracker.scrapper.model.Link;
import java.util.List;

public record LinkWithChats(Link link, List<Long> chatIds) {}
