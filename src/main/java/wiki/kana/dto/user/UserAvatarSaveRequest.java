package wiki.kana.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存用户头像请求（OSS 公共读直链）
 */
@Data
public class UserAvatarSaveRequest {

    @NotBlank(message = "头像URL不能为空")
    @Size(max = 500, message = "头像URL长度不超过500字符")
    @Pattern(regexp = "^https?://\\S+$", message = "头像URL格式不正确")
    private String publicUrl;
}

