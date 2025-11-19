package wiki.kana.dto.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import wiki.kana.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long id;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private Post.PostStatus status;
    private Boolean isFeatured;
    private Integer viewCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AuthorDto author;
    private CategoryDto category;
    private List<TagDto> tags;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorDto {
        private Long id;
        private String username;
        private String nickname;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDto {
        private Long id;
        private String name;
        private String slug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TagDto {
        private Long id;
        private String name;
        private String slug;
    }
}
