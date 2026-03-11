package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.grpc.ScrapperGrpcClient;
import com.pengrad.telegrambot.TelegramBot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UpdateControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ScrapperGrpcClient scrapperGrpcClient;

    @MockitoBean
    TelegramBot telegramBot;

    // valid POST /updates: 200 OK
    @Test
    void validUpdate_returns200() throws Exception {
        mockMvc.perform(post("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "id": 1,
                        "url": "https://github.com/user/repo",
                        "description": "Update",
                        "tgChatIds": [123]
                    }
                    """))
            .andExpect(status().isOk());
    }

    // missing url: 400 Bad Request
    @Test
    void missingUrl_returns400() throws Exception {
        mockMvc.perform(post("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "id": 1,
                        "description": "Update",
                        "tgChatIds": [123]
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    // empty tgChatIds: 400 Bad Request
    @Test
    void emptyTgChatIds_returns400() throws Exception {
        mockMvc.perform(post("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "id": 1,
                        "url": "https://github.com/user/repo",
                        "description": "Update",
                        "tgChatIds": []
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    // empty body: 400 Bad Request
    @Test
    void emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/updates")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
