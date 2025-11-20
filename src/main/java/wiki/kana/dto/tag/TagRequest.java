package wiki.kana.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签创建请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagRequest {

    @NotBlank(message = "标签名称不能为空")
    @Size(min = 2, max = 100, message = "标签名称长度需在2-100个字符之间")
    private String name;

    @Size(max = 200, message = "Slug长度不能超过200个字符")
    private String slug;

    @Size(max = 500, message = "标签描述不能超过500个字符")
    private String description;

    @Size(max = 7, message = "颜色值格式不正确")
    private String color;
}
