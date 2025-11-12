package wiki.kana.exception;

/**
 * 资源不存在异常
 * 当尝试访问不存在的资源时抛出
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
