package hdc.company.monitor.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Objects;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitWebConfig(locations = "/test-context.xml")
public class ManageControllerTest {

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
    public void shouldReturnManageView() throws Exception {
        mockMvc.perform(get("/manage"))
               .andExpect(status().isOk())
               .andExpect(view().name("manage"))
               .andExpect(model().attributeExists("version"))
               .andExpect(model().attributeExists("productList"));
    }

    @Test
    @WithMockUser
    public void shouldHandleCreateProduct() throws Exception {
        mockMvc.perform(post("/manage/create")
                .with(csrf())
                .param("productName", "newProd")
                .param("productDescription", "newDesc")
                .param("testCase", "newTC"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/manage"));
    }

    @Test
    @WithMockUser
    public void shouldHandleUpdateProduct() throws Exception {
        mockMvc.perform(post("/manage/update")
                .with(csrf())
                .param("productName", "prod1")
                .param("productDescription", "updatedDesc"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/manage"));
    }

    @Test
    @WithMockUser
    public void shouldHandleDeleteProduct() throws Exception {
        mockMvc.perform(post("/manage/delete")
                .with(csrf())
                .param("productName", "prod1"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrl("/manage"));
    }
}
