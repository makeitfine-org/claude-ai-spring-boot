package pl.piomin.services.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pl.piomin.services.model.Worker;
import pl.piomin.services.service.WorkerService;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class WorkerControllerTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    WorkerService workerService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void getAll_withValidJwt_returns200() throws Exception {
        given(workerService.findAll()).willReturn(List.of(new Worker(1L, "Jan", "Kowalski", 30)));

        mockMvc.perform(get("/api/workers").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Jan"))
                .andExpect(jsonPath("$[0].surname").value("Kowalski"));
    }

    @Test
    void getAll_withNoJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/workers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_withValidJwt_returns200() throws Exception {
        given(workerService.findById(1L)).willReturn(new Worker(1L, "Jan", "Kowalski", 30));

        mockMvc.perform(get("/api/workers/1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jan"));
    }

    @Test
    void getById_withNoJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/workers/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withValidJwt_returns201() throws Exception {
        Worker saved = new Worker(1L, "Jan", "Kowalski", 30);
        given(workerService.create(any())).willReturn(saved);

        mockMvc.perform(post("/api/workers")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jan\",\"surname\":\"Kowalski\",\"age\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_withNoJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/workers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jan\",\"surname\":\"Kowalski\",\"age\":30}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_withValidJwt_returns200() throws Exception {
        Worker updated = new Worker(1L, "Anna", "Nowak", 25);
        given(workerService.update(eq(1L), any())).willReturn(updated);

        mockMvc.perform(put("/api/workers/1")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Anna\",\"surname\":\"Nowak\",\"age\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Anna"));
    }

    @Test
    void delete_withValidJwt_returns204() throws Exception {
        willDoNothing().given(workerService).delete(1L);

        mockMvc.perform(delete("/api/workers/1").with(jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_withNoJwt_returns401() throws Exception {
        mockMvc.perform(delete("/api/workers/1"))
                .andExpect(status().isUnauthorized());
    }
}
