package wiki.kana.dto.theme;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 主题响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String version;
    private String author;
    private String previewUrl;
    private Boolean active;
    private Boolean isDefault;
    private Map<String, Object> config;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
