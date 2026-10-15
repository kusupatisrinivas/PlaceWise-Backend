package com.placewise;

import com.placewise.config.SecurityConfig;
import com.placewise.controller.HealthController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.jwt-secret=health-test-only-secret-longer-than-thirty-two-bytes",
    "app.timezone=UTC",
    "app.cors-origins=https://place-wise-frontend.vercel.app"
})
class HealthApiTest {
    private static final String FRONTEND = "https://place-wise-frontend.vercel.app";
    @Autowired MockMvc mvc;
    @MockitoBean ApplicationAvailability availability;

    @Test void readyEndpointIsPublicAndNotCacheable() throws Exception {
        when(availability.getReadinessState()).thenReturn(ReadinessState.ACCEPTING_TRAFFIC);
        mvc.perform(get("/api/health").header("Origin", FRONTEND))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().json("{\"status\":\"UP\"}"));
    }

    @Test void startupDoesNotReportReady() throws Exception {
        when(availability.getReadinessState()).thenReturn(ReadinessState.REFUSING_TRAFFIC);
        mvc.perform(get("/api/health").header("Origin", FRONTEND))
            .andExpect(status().isServiceUnavailable())
            .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().json("{\"status\":\"STARTING\"}"));
    }

    @Test void frontendCanPreflightHealthRequest() throws Exception {
        mvc.perform(options("/api/health").header("Origin", FRONTEND)
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", FRONTEND));
    }

    @Test void unconfiguredOriginIsRejected() throws Exception {
        mvc.perform(get("/api/health").header("Origin", "https://untrusted.example"))
            .andExpect(status().isForbidden())
            .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test void publicHealthRuleDoesNotExposeOtherMethodsOrRoutes() throws Exception {
        mvc.perform(post("/api/health")).andExpect(status().isUnauthorized());
        mvc.perform(head("/api/health")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/health/details")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/companies")).andExpect(status().isUnauthorized());
    }
}
