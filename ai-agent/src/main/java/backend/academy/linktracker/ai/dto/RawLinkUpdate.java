package backend.academy.linktracker.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// message from link.raw-updates
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawLinkUpdate(long id, String description, String author, List<Long> tgChatIds) {}
