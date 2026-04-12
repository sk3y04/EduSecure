package edusecure.edusecure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;

import edusecure.edusecure.service.spacechat.DisabledSpaceChatService;
import edusecure.edusecure.service.spacechat.SpaceChatService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EduSecureApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SpaceChatService spaceChatService;

    @Test
    void contextLoads() {
    }

    @Test
    void healthEndpointIsPublicAndReturnsUp() throws Exception {
        mockMvc.perform(get("/api/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void chatDisabledByDefaultDoesNotCreateMongoInfrastructure() {
        assertThat(applicationContext.containsBean("mongoTemplate")).isFalse();
        assertThat(applicationContext.getBeansOfType(MongoTemplate.class)).isEmpty();
        assertThat(spaceChatService).isInstanceOf(DisabledSpaceChatService.class);
    }

}
