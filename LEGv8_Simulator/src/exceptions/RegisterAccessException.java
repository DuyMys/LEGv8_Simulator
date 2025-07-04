package exceptions;

/**
 * Ngoại lệ được ném khi có lỗi truy cập thanh ghi, ví dụ: chỉ số không hợp lệ.
 */
public class RegisterAccessException extends RuntimeException {
    public RegisterAccessException(String message) {
        super(message);
    }

    public RegisterAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}