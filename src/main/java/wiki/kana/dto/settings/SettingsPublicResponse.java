package wiki.kana.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对外公开的系统设置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsPublicResponse {

    private String blogName;
    private String blogDescription;
    private String blogTagline;
    private String authorName;
    private String authorEmail;
    private Integer pageSize;
    private Boolean commentEnabled;
    private String theme;
}
