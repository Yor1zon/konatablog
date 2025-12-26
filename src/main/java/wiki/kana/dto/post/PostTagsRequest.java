package wiki.kana.dto.post;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章标签设置请求DTO
 */
@Data
@Accessors(chain = true)
public class PostTagsRequest {

    private List<Long> tagIds = new ArrayList<>();
}