package wiki.kana.dto.category;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分类排序调整请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryReorderRequest {

    @NotEmpty(message = "排序数据不能为空")
    @Valid
    private List<OrderItem> orders;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        @NotNull(message = "分类ID不能为空")
        private Long id;

        @NotNull(message = "排序值不能为空")
        private Integer order;
    }
}
