package wiki.kana.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类树响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeResponse {

    private Long id;
    private String name;
    private String slug;
    private Long parentId;
    private Long postCount;
    @Builder.Default
    private List<CategoryTreeResponse> children = new ArrayList<>();
}
