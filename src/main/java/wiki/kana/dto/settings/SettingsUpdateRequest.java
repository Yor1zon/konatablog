package wiki.kana.dto.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统设置更新请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsUpdateRequest {

    @Size(max = 100, message = "博客名称不能超过100个字符")
    private String blogName;

    @Size(max = 200, message = "博客副标题不能超过200个字符")
    private String blogTagline;

    @Size(max = 500, message = "博客描述不能超过500个字符")
    private String blogDescription;

    @Size(max = 100, message = "作者名称不能超过100个字符")
    private String authorName;

    @Email(message = "作者邮箱格式不正确")
    private String authorEmail;

    @Min(value = 1, message = "页大小至少为1")
    @Max(value = 100, message = "页大小不能超过100")
    private Integer pageSize;

    private Boolean commentEnabled;

    @Size(max = 100, message = "主题标识长度不能超过100")
    private String theme;
}
