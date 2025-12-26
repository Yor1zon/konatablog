package wiki.kana.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户信息更新请求
 */
@Data
public class UserUpdateRequest {

    /**
     * 用户名
     */
    @Size(min = 1, max = 50, message = "用户名长度需在1-50之间")
    private String username;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    @Size(max = 150, message = "邮箱长度不超过150字符")
    private String email;

    /**
     * 昵称（显示名称）
     */
    @Size(max = 100, message = "昵称长度不超过100字符")
    private String nickname;

    /**
     * 新密码（可选，仅在修改密码时提供）
     */
    @Size(min = 6, message = "密码长度不能少于6位")
    private String password;

    /**
     * 确认新密码
     */
    private String confirmPassword;
}
