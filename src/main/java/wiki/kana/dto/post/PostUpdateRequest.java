package wiki.kana.dto.post;

import lombok.Data;
import wiki.kana.entity.Post;

import java.util.List;

/**
 * 更新博客的请求体
 */
@Data
public class PostUpdateRequest {

    private String title;

    private String content;

    private String excerpt;

    private Post.PostStatus status;

    private Long categoryId;

    private List<Long> tagIds;

    private Boolean isFeatured;

    private String slug;
}
