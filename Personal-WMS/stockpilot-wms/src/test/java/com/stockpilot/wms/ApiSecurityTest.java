package com.stockpilot.wms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    private static final String LOGIN_BODY = """
            {"appName":"Test suite","appId":"test-1","username":"connector","password":"%s"}
            """;

    @Test
    @DisplayName("API calls without a token are rejected with 401")
    void apiRequiresToken() throws Exception {
        mvc.perform(get("/api/parts")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logging in returns a token that unlocks the API")
    void loginThenCallApi() throws Exception {
        String response = mvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY.formatted("connector123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);

        mvc.perform(get("/api/parts").header("Authorization", "Bearer " + json.get("token").asText()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A wrong password gets 401, not a token")
    void wrongPassword() throws Exception {
        mvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY.formatted("nope")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Office pages send signed-out visitors to the login page")
    void webRequiresLogin() throws Exception {
        mvc.perform(get("/parts")).andExpect(status().is3xxRedirection());
    }
}
