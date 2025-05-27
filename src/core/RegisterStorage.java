//package core;
/**
 * Simulates the LEGv8 register file storage (X0–X31).
 */
public class RegisterStorage {
    private final long[] registers;

    public RegisterStorage() {
        registers = new long[32]; // X0–X31
        registers[31] = 0; // XZR is always 0
    }

    public long read(int index) {
        if (index < 0 || index > 31) {
            throw new IllegalArgumentException("Invalid register index: " + index);
        }
        return index == 31 ? 0 : registers[index];
    }

    public void write(int index, long value) {
        if (index < 0 || index > 31) {
            throw new IllegalArgumentException("Invalid register index: " + index);
        }
        if (index != 31) { // XZR is read-only
            registers[index] = value;
        }
    }
}
