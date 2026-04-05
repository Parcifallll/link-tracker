package backend.academy.linktracker.scrapper.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import backend.academy.linktracker.scrapper.AbstractIntegrationTest;
import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ScrapperControllerTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

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

    @Test
    void deleteNonExistentChat_returns404() throws Exception {
        mockMvc.perform(delete("/tg-chat/99999")).andExpect(status().isNotFound());
    }

    @Test
    void registerChatTwice_returns409() throws Exception {
        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isOk());

        mockMvc.perform(post("/tg-chat/1")).andExpect(status().isConflict());
    }
}
