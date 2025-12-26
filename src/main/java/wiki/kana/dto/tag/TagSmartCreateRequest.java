package wiki.kana.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 智能创建标签请求DTO
 */
@Data
@Accessors(chain = true)
public class TagSmartCreateRequest {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称长度不能超过50个字符")
    private String name;

    @Size(max = 200, message = "标签描述长度不能超过200个字符")
    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$|^$", message = "颜色格式必须是十六进制颜色码或为空")
    private String color;
}