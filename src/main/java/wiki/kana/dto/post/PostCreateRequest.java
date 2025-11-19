package wiki.kana.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import wiki.kana.entity.Post;

import java.util.List;

/**
 * 创建博客的请求体
 */
@Data
public class PostCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String excerpt;

    private Post.PostStatus status;

    private Long categoryId;

    private List<Long> tagIds;

    private Boolean isFeatured;

    private String slug;
}
