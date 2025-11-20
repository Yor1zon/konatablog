package wiki.kana.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分类统计响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatsResponse {

    private long totalCategories;
    private long categoriesWithPosts;
    private long emptyCategories;
    private List<CategorySummary> topCategories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySummary {
        private Long id;
        private String name;
        private Long postCount;
    }
}
