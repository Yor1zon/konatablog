package wiki.kana.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类创建请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    @Size(min = 2, max = 50, message = "分类名称长度需在2-50个字符之间")
    private String name;

    @Size(max = 200, message = "Slug长度不能超过200个字符")
    private String slug;

    @Size(max = 500, message = "分类描述不能超过500个字符")
    private String description;

    private Long parentId;

    private Integer sortOrder;

    private Boolean isActive;
}
