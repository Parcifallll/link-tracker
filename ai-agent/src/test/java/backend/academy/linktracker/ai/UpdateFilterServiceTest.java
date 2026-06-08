package backend.academy.linktracker.ai;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.ai.dto.RawLinkUpdate;
import backend.academy.linktracker.ai.properties.AiAgentProperties;
import backend.academy.linktracker.ai.properties.AiAgentProperties.Filtering;
import backend.academy.linktracker.ai.properties.AiAgentProperties.Summarization;
import backend.academy.linktracker.ai.service.UpdateFilterService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateFilterServiceTest {

    private UpdateFilterService filterService;

    @BeforeEach
    void setUp() {
        var properties = new AiAgentProperties(
                new Filtering(List.of("spam", "ads", "promo"), List.of("bot-user", "spammer"), 20),
                new Summarization(500));
        filterService = new UpdateFilterService(properties);
    }

    @Test
    void stopWord_inDescription_updateFiltered() {
        assertThat(filterService.passes(
                        new RawLinkUpdate(1L, "This is a spam message that is long enough", "alice", List.of(1L))))
                .isFalse();
    }

    @Test
    void stopWord_caseInsensitive_updateFiltered() {
        assertThat(filterService.passes(
                        new RawLinkUpdate(1L, "This is a SPAM message that is long enough", "alice", List.of(1L))))
                .isFalse();
    }

    @Test
    void multipleStopWords_anyOnePresent_updateFiltered() {
        assertThat(filterService.passes(
                        new RawLinkUpdate(1L, "Check out these great promo deals today", "alice", List.of(1L))))
                .isFalse();
    }

    @Test
    void excludedAuthor_updateFiltered() {
        assertThat(filterService.passes(
                        new RawLinkUpdate(1L, "This is a perfectly valid update message", "bot-user", List.of(1L))))
                .isFalse();
    }

    @Test
    void excludedAuthor_caseInsensitive_updateFiltered() {
        assertThat(filterService.passes(
                        new RawLinkUpdate(1L, "This is a perfectly valid update message", "BOT-USER", List.of(1L))))
                .isFalse();
    }

    @Test
    void nullAuthor_doesNotCrash_passesAuthorFilter() {
        assertThat(filterService.passes(
                        new RawLinkUpdate(1L, "This is a perfectly valid update message", null, List.of(1L))))
                .isTrue();
    }

    @Test
    void descriptionTooShort_updateFiltered() {
        assertThat(filterService.passes(new RawLinkUpdate(1L, "Too short", "alice", List.of(1L))))
                .isFalse();
    }

    @Test
    void descriptionExactlyMinLength_passes() {
        assertThat(filterService.passes(new RawLinkUpdate(1L, "a".repeat(20), "alice", List.of(1L))))
                .isTrue();
    }

    @Test
    void validUpdate_passesAllFilters() {
        assertThat(filterService.passes(new RawLinkUpdate(
                        1L, "New issue opened in the repository with detailed description", "alice", List.of(1L))))
                .isTrue();
    }

    @Test
    void nullDescription_updateFiltered() {
        assertThat(filterService.passes(new RawLinkUpdate(1L, null, "alice", List.of(1L))))
                .isFalse();
    }
}
