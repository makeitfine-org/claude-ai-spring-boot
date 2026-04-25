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
import pl.piomin.services.model.Work;
import pl.piomin.services.model.Worker;
import pl.piomin.services.service.WorkService;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class WorkControllerTest {

    @Autowired
    WebApplicationContext context;

    @MockitoBean
    WorkService workService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private Worker worker() {
        return new Worker(1L, "Jan", "Kowalski", 30);
    }

    private Work work() {
        return new Work(1L, "Fix bug", "Critical fix", LocalDate.of(2026, 6, 1),
                BigDecimal.valueOf(500), LocalDate.of(2026, 6, 15), worker());
    }

    @Test
    void getAll_withValidJwt_returns200() throws Exception {
        given(workService.findAll()).willReturn(List.of(work()));

        mockMvc.perform(get("/api/works").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Fix bug"));
    }

    @Test
    void getAll_withNoJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/works"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_withValidJwt_returns200() throws Exception {
        given(workService.findById(1L)).willReturn(work());

        mockMvc.perform(get("/api/works/1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Fix bug"));
    }

    @Test
    void getById_withNoJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/works/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withValidJwt_returns201() throws Exception {
        given(workService.create(any())).willReturn(work());

        mockMvc.perform(post("/api/works")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Fix bug\",\"description\":\"Critical fix\"," +
                                "\"endDate\":\"2026-06-01\",\"price\":500," +
                                "\"payDate\":\"2026-06-15\",\"assignedWorkerId\":1," +
                                "\"additionalWorkerIds\":[]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_withNoJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/works")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Fix bug\",\"assignedWorkerId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_withValidJwt_returns200() throws Exception {
        Work updated = new Work(1L, "Updated", "Updated desc", LocalDate.of(2026, 7, 1),
                BigDecimal.valueOf(750), null, worker());
        given(workService.update(eq(1L), any())).willReturn(updated);

        mockMvc.perform(put("/api/works/1")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\",\"description\":\"Updated desc\"," +
                                "\"endDate\":\"2026-07-01\",\"price\":750,\"assignedWorkerId\":1," +
                                "\"additionalWorkerIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void delete_withValidJwt_returns204() throws Exception {
        willDoNothing().given(workService).delete(1L);

        mockMvc.perform(delete("/api/works/1").with(jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_withNoJwt_returns401() throws Exception {
        mockMvc.perform(delete("/api/works/1"))
                .andExpect(status().isUnauthorized());
    }
}
