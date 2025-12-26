package wiki.kana.dto.post;

import lombok.Builder;
import lombok.Data;
import wiki.kana.entity.Post;

import java.time.LocalDateTime;

/**
 * 文章简要信息响应DTO
 */
@Data
@Builder
public class PostSummaryResponse {
    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private Post.PostStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从Post实体转换为响应DTO
     */
    public static PostSummaryResponse fromPost(Post post) {
        return PostSummaryResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .excerpt(post.getExcerpt())
                .status(post.getStatus())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}