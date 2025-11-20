package wiki.kana.dto.tag;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 标签批量创建请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagBulkRequest {

    @NotEmpty(message = "标签名称列表不能为空")
    private List<String> names;
}
