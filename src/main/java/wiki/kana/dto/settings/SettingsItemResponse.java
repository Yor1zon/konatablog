package wiki.kana.dto.settings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统设置项 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettingsItemResponse {

    private Long id;
    private String key;
    private String value;
    private String group;
    private String description;
    private String optionType;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
