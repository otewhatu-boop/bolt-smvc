package hdc.company.monitor.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import hdc.company.monitor.config.SecurityConfig;
import java.util.Objects;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(locations = {"/test-context.xml"})
@org.springframework.context.annotation.Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "entra.client.id=",
        "entra.client.secret=",
        "entra.tenant.id="
})
public class HomeControllerTest {

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
    public void shouldReturnHomeView() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().isOk())
               .andExpect(view().name("home"))
               .andExpect(model().attributeExists("message"));
    }

    @Test
    public void shouldReturnLoginView() throws Exception {
        mockMvc.perform(get("/login"))
               .andExpect(status().isOk())
               .andExpect(view().name("login"))
               .andExpect(model().attributeExists("version"))
               .andExpect(model().attributeExists("entraEnabled"))
               .andExpect(model().attributeExists("entraWarning"));
    }

    @Test
    public void shouldReturnFaviconSvg() throws Exception {
        mockMvc.perform(get("/favicon.svg"))
               .andExpect(status().isOk())
               .andExpect(content().contentType("image/svg+xml"));
    }

    @Test
    public void shouldReturnFaviconIco() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
               .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnFavicon32() throws Exception {
        mockMvc.perform(get("/favicon-32x32.png"))
               .andExpect(status().isOk());
    }
}