package wiki.kana.dto.tag;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 标签文章关联请求DTO
 */
@Data
@Accessors(chain = true)
public class TagPostsRequest {

    @NotEmpty(message = "文章ID列表不能为空")
    private List<Long> postIds;
}