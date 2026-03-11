package backend.academy.linktracker.scrapper.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ScrapperControllerTest {

    @Autowired
    MockMvc mockMvc;

    // Test 3.1: регистрация чата + добавление ссылки + получение ссылки
    @Test
    void addAndGetLink_returnsLink() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "link": "https://github.com/user/repo",
                                "tags": [],
                                "filters": []
                            }
                            """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[*].url", hasItem("https://github.com/user/repo")));
    }

    // Test 3.2: добавление + удаление ссылки -> ссылки нет в списке
    @Test
    void addAndDeleteLink_linkRemovedFromList() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "link": "https://github.com/user/repo",
                                "tags": [],
                                "filters": []
                            }
                            """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "link": "https://github.com/user/repo"
                            }
                            """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[*].url", not(hasItem("https://github.com/user/repo"))));
    }

    // Test 3.3: удаление ссылки из несуществующего чата -> 404, оригинальная ссылка сохранилась
    @Test
    void deleteLinkFromNonExistentChat_returns404_originalLinkPresent() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "link": "https://github.com/user/repo",
                                "tags": [],
                                "filters": []
                            }
                            """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/links")
                        .header("Tg-Chat-Id", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "link": "https://github.com/user/repo"
                            }
                            """))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/links").header("Tg-Chat-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links[*].url", hasItem("https://github.com/user/repo")));
    }

    // Test 3.4: добавление ссылки в несуществующий чат -> не 200
    @Test
    void addLinkToNonExistentChat_returnsError() throws Exception {
        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "link": "https://github.com/user/repo",
                                "tags": [],
                                "filters": []
                            }
                            """))
                .andExpect(status().isNotFound());
    }

    // Test 3.5: регистрация + удаление чата -> добавление ссылки не 200
    @Test
    void addLinkToDeletedChat_returnsError() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(delete("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/links")
                        .header("Tg-Chat-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "link": "https://github.com/user/repo",
                                "tags": [],
                                "filters": []
                            }
                            """))
                .andExpect(status().isNotFound());
    }

    // Test 3.6: удаление несуществующего чата -> 404
    @Test
    void deleteNonExistentChat_returns404() throws Exception {
        mockMvc.perform(delete("/tg-chat/99999")).andExpect(status().isNotFound());
    }

    // дополнительно: повторная регистрация чата -> 409
    @Test
    void registerChatTwice_returns409() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isConflict());
    }
}
