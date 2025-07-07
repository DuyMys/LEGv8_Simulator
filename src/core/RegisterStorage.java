package core;

import java.util.HashMap;
import java.util.Map;

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

    public void reset() {
        for (int i = 0; i < registers.length; i++) {
            registers[i] = 0; 
        }
        registers[31] = 0; // Ensure XZR remains 0
    }
    /**
     * Retrieves a snapshot of all register values.
     * @return A Map where the key is the register number (0-31) and the value is its 64-bit content.
     */
    public Map<Integer, Long> getAllRegisters() {
        Map<Integer, Long> allRegisters = new HashMap<>();
        for (int i = 0; i < 32; i++) {
            // Ensure we correctly report XZR (X31) as 0, even if the array somehow held a different value.
            if (i == 31) {
                allRegisters.put(i, 0L);
            } else {
                allRegisters.put(i, registers[i]);
            }
        }
        return allRegisters;
    }
}
