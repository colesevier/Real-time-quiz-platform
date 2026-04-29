package com.quiz.controller;

import com.quiz.dao.UserDAO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@WebMvcTest(AuthController.class)
public class AuthControllerGuestTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserDAO userDAO; // not used in guest flow but required by controller constructor

    @Test
    void guestFormSetsSessionAndRedirects() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/auth/guest").param("guestName", "Alice").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/join"));

        Object val = session.getAttribute("guestName");
        assertThat(val).isEqualTo("Alice");
    }

    @Test
    void guestFormEmptyShowsLoginWithError() throws Exception {
        mvc.perform(post("/auth/guest").param("guestName", "")).andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeExists("guestError"));
    }
}
