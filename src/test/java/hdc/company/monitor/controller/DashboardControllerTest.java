package hdc.company.monitor.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Objects;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(locations = "/test-context.xml")
public class DashboardControllerTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(Objects.requireNonNull(this.wac))
                .apply(Objects.requireNonNull(springSecurity()))
                .build();
    }

    @Test
    @WithMockUser
    public void shouldReturnDashboardView() throws Exception {
        mockMvc.perform(get("/dashboard"))
               .andExpect(status().isOk())
               .andExpect(view().name("dashboard"))
               .andExpect(model().attributeExists("version"))
               .andExpect(model().attributeExists("systemStatusList"));
    }

    @Test
    @WithMockUser
    public void shouldHandleDeleteStatus() throws Exception {
        mockMvc.perform(post("/dashboard/delete")
                .param("systemId", "sys1")
                .param("testCase", "tc1")
                .with(csrf()))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/dashboard"));
    }
}