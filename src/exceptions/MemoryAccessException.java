package exceptions;

/**
 * Ngoại lệ được ném khi có lỗi truy cập bộ nhớ, ví dụ: địa chỉ không hợp lệ.
 */
public class MemoryAccessException extends RuntimeException {
    public MemoryAccessException(String message) {
        super(message);
    }

    public MemoryAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}