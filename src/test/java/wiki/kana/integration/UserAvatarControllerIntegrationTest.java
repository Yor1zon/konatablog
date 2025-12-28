package wiki.kana.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import wiki.kana.entity.User;
import wiki.kana.service.UserService;
import wiki.kana.util.JwtTokenUtil;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.application.name=konatablog-avatar-test",
        "spring.datasource.url=jdbc:sqlite:./target/konatablog-avatar-test.db",
        "spring.datasource.driver-class-name=org.sqlite.JDBC",
        "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "server.port=0",
        "app.jwt.secret=konatablog-jwt-secret-key-for-avatar-test-environment-change-this-string-please",
        "app.jwt.expiration=86400"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserAvatarControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Test
    @DisplayName("保存头像URL：更新当前用户 avatarUrl")
    void saveAvatarShouldUpdateCurrentUserAvatarUrl() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        User created = userService.createUser(User.builder()
                .username("avatar_user_" + suffix)
                .password("testPassword123")
                .email("avatar_" + suffix + "@example.com")
                .role(User.UserRole.USER)
                .isActive(true)
                .build());

        String token = jwtTokenUtil.generateToken(created.getId(), created.getUsername(), created.getRole().name());
        String avatarUrl = "https://example.com/avatars/" + suffix + ".png";

        mockMvc.perform(post("/api/users/me/avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(Map.of("publicUrl", avatarUrl))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.avatarUrl").value(avatarUrl));

        User updated = userService.findById(created.getId());
        assertThat(updated.getAvatarUrl()).isEqualTo(avatarUrl);
    }
}
