package backend.academy.linktracker.ai.dto;

import java.util.List;

// message to link.processed-updates
public record ProcessedLinkUpdate(long id, String description, List<Long> tgChatIds, String priority) {

    public static ProcessedLinkUpdate from(RawLinkUpdate raw, String processedDescription) {
        return new ProcessedLinkUpdate(raw.id(), processedDescription, raw.tgChatIds(), "HIGH");
    }
}
