package wiki.kana.exception;

/**
 * 标签绑定异常
 * 用于处理标签与文章关联过程中的异常情况
 */
public class TagBindingException extends RuntimeException {

    private final String errorCode;

    public TagBindingException(String message) {
        super(message);
        this.errorCode = "TAG_BINDING_ERROR";
    }

    public TagBindingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TagBindingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "TAG_BINDING_ERROR";
    }

    public TagBindingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}