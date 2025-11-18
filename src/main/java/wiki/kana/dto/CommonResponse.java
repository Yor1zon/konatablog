package wiki.kana.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse<T> {

    private Boolean success;
    private T data;
    private String message;
    private ErrorInfo error;

    /**
     * 成功响应（带数据）
     */
    public static <T> CommonResponse<T> success(T data) {
        return CommonResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * 成功响应（带消息）
     */
    public static <T> CommonResponse<T> success(T data, String message) {
        return CommonResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    /**
     * 成功响应（仅消息）
     */
    public static <T> CommonResponse<T> success(String message) {
        return CommonResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * 错误响应
     */
    public static <T> CommonResponse<T> error(String code, String message) {
        return CommonResponse.<T>builder()
                .success(false)
                .error(new ErrorInfo(code, message))
                .build();
    }

    /**
     * 错误信息类
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorInfo {
        private String code;
        private String message;
    }
}