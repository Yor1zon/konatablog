package wiki.kana.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 保存用户头像响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAvatarSaveResponse {
    private String avatarUrl;
}

