package com.techatlas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
public class SearchRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testSearchRegression() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/search").param("q", "java"))
                .andDo(print())
                .andReturn();
        System.out.println("STATUS CODE: " + result.getResponse().getStatus());
        System.out.println("RESPONSE BODY: " + result.getResponse().getContentAsString());
    }
}
