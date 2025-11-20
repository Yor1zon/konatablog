package wiki.kana.dto.post;

import org.springframework.util.CollectionUtils;
import wiki.kana.entity.Post;
import wiki.kana.entity.Tag;

import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Post实体与DTO转换工具
 */
public final class PostMapper {

    private PostMapper() {
    }

    public static PostResponse toPostResponse(Post post) {
        if (post == null) {
            return null;
        }

        PostResponse response = PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .content(post.getContent())
                .excerpt(post.getExcerpt())
                .status(post.getStatus())
                .isFeatured(post.getIsFeatured())
                .viewCount(post.getViewCount())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();

        if (post.getAuthor() != null) {
            response.setAuthor(PostResponse.AuthorDto.builder()
                    .id(post.getAuthor().getId())
                    .username(post.getAuthor().getUsername())
                    .nickname(post.getAuthor().getDisplayName())
                    .build());
        }

        if (post.getCategory() != null) {
            response.setCategory(PostResponse.CategoryDto.builder()
                    .id(post.getCategory().getId())
                    .name(post.getCategory().getName())
                    .slug(post.getCategory().getSlug())
                    .build());
        }

        if (!CollectionUtils.isEmpty(post.getTags())) {
            response.setTags(post.getTags().stream()
                    .map(PostMapper::toTagDto)
                    .collect(Collectors.toList()));
        } else {
            response.setTags(Collections.emptyList());
        }

        return response;
    }

    private static PostResponse.TagDto toTagDto(Tag tag) {
        return PostResponse.TagDto.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .build();
    }
}
