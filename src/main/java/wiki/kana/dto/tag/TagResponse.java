package wiki.kana.dto.tag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private Integer usageCount;
    private Integer postCount;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
