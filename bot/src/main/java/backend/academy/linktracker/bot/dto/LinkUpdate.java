package backend.academy.linktracker.bot.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;

public record LinkUpdate(
        @NotNull long id,
        @NotNull URI url,
        String description,
        @NotNull @NotEmpty List<Long> tgChatIds) {}
