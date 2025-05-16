package core.cpu;

import java.util.HashMap;
import java.util.Map;

public class Registers {
    private static final int NUM_REGISTERS = 32;
    private final long[] registers = new long[NUM_REGISTERS];
    private static final Map<String, Integer> nameToIndex = new HashMap<>();

    public static final int XZR = 31;
    public static final int SP = 28;

    static {
        // X0 - X30
        for (int i = 0; i < 31; i++) {
            nameToIndex.put("X" + i, i);
        }
        nameToIndex.put("SP", SP);    // Stack Pointer
        nameToIndex.put("XZR", XZR);  // Zero Register
        nameToIndex.put("X31", XZR);  // if use X31
    }

    public long read(int index) {
        validateIndex(index);
        return (index == XZR) ? 0 : registers[index];
    }

    public void write(int index, long value) {
        validateIndex(index);
        if (index != XZR) {
            registers[index] = value;
        }
    }

    public long read(String name) {
        return read(resolveName(name));
    }

    public void write(String name, long value) {
        write(resolveName(name), value);
    }

    public void dump() {
        for (int i = 0; i < NUM_REGISTERS; i++) {
            String name = getRegisterName(i);
            System.out.printf("%-4s = 0x%016X%n", name, read(i));
        }
    }

    private int resolveName(String name) {
        Integer index = nameToIndex.get(name.toUpperCase());
        if (index == null) {
            throw new IllegalArgumentException("Invalid register name: " + name);
        }
        return index;
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= NUM_REGISTERS) {
            throw new IllegalArgumentException("Invalid register index: X" + index);
        }
    }

    private String getRegisterName(int index) {
        if (index == SP) return "SP";
        if (index == XZR) return "XZR";
        return "X" + index;
    }
}
