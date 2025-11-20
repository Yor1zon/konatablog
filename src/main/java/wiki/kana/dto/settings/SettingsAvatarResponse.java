package wiki.kana.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 头像上传响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsAvatarResponse {

    private String avatarUrl;
}
