package wiki.kana.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username; // 可以是用户名或邮箱

    @NotBlank(message = "密码不能为空")
    private String password;
}