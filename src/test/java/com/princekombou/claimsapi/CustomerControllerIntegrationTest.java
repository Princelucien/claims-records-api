package com.princekombou.claimsapi;

import com.princekombou.claimsapi.dto.CustomerCreateRequest;
import com.princekombou.claimsapi.dto.RiskFlagCreateRequest;
import com.princekombou.claimsapi.model.RiskFlag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack integration tests: real Spring context, real MockMvc dispatch through
 * the controller layer, real Postgres via the same connection the app uses.
 * No mocks. Each test runs in a transaction that rolls back afterward, so the
 * database is left exactly as it was found regardless of test outcome.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createCustomer_thenAddRiskFlag_thenSummaryReflectsIt() throws Exception {
        CustomerCreateRequest createRequest =
                new CustomerCreateRequest("Test Customer", "test.customer." + System.nanoTime() + "@example.com");

        String responseJson = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(createRequest.email()))
                .andReturn().getResponse().getContentAsString();

        long customerId = objectMapper.readTree(responseJson).get("id").asLong();

        RiskFlagCreateRequest flagRequest =
                new RiskFlagCreateRequest("Identity Verification", RiskFlag.Severity.HIGH, "Automated test flag");

        mockMvc.perform(post("/api/customers/{id}/risk-flags", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flagRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.severity").value("HIGH"));

        mockMvc.perform(get("/api/customers/{id}/risk-summary", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openFlagCount").value(1))
                .andExpect(jsonPath("$.openBySeverity.HIGH").value(1));
    }

    @Test
    void createCustomer_duplicateEmail_returns409() throws Exception {
        String email = "duplicate." + System.nanoTime() + "@example.com";
        CustomerCreateRequest request = new CustomerCreateRequest("First", email);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void createCustomer_invalidEmail_returns400WithFieldError() throws Exception {
        CustomerCreateRequest request = new CustomerCreateRequest("Bad Email", "not-an-email");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void getCustomer_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/customers/{id}", 999_999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
