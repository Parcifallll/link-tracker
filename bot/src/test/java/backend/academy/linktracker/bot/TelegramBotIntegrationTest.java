package backend.academy.linktracker.bot;

import backend.academy.linktracker.bot.properties.TelegramProperties;
import com.pengrad.telegrambot.TelegramBot;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@EnableWireMock
class TelegramBotIntegrationTest implements WithAssertions {

    @Autowired
    TelegramBot telegramBot;

    @Autowired
    TelegramProperties telegramProperties;

    @AfterEach
    void clearUpdatesListener() {
        telegramBot.removeGetUpdatesListener();
    }

    // @Test
    // void nonExistingTokenRequest() { ... }

    // @Test
    // void updatesListenerReceivesUpdates() { ... }
}
