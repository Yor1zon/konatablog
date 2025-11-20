package wiki.kana.dto.theme;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 主题配置请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeConfigRequest {

    @NotNull(message = "配置内容不能为空")
    private Map<String, Object> config;
}
