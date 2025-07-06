package exceptions;

/**
 * Ngoại lệ được ném khi gặp lệnh không hợp lệ trong quá trình mô phỏng LEGv8.
 */
public class InvalidInstructionException extends RuntimeException {
    /**
     * Tạo ngoại lệ với thông báo lỗi.
     * 
     * @param message Thông báo mô tả lỗi.
     */
    public InvalidInstructionException(String message) {
        super(message);
    }

    /**
     * Tạo ngoại lệ với thông báo và nguyên nhân.
     * 
     * @param message Thông báo mô tả lỗi.
     * @param cause   Nguyên nhân gây ra lỗi.
     */
    public InvalidInstructionException(String message, Throwable cause) {
        super(message, cause);
    }
}