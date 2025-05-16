package core.cpu;

/**
 * Quản lý các cờ N (âm), Z (zero), C (carry), V (overflow).
 * Được cập nhật bởi ALU sau mỗi phép toán logic/số học.
 */
public class Flags {
    private boolean negative;
    private boolean zero;
    private boolean carry;
    private boolean overflow;

    public void updateFrom(boolean n, boolean z, boolean c, boolean v) {
        this.negative = n;
        this.zero = z;
        this.carry = c;
        this.overflow = v;
    }

    public void reset() {
        negative = zero = carry = overflow = false;
    }

    public boolean isNegative() { return negative; }
    public boolean isZero()     { return zero; }
    public boolean isCarry()    { return carry; }
    public boolean isOverflow() { return overflow; }

    @Override
    public String toString() {
        return String.format("N=%b Z=%b C=%b V=%b", negative, zero, carry, overflow);
    }
}
