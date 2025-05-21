package core.cpu;

public class Flags {
    private boolean negative;
    private boolean zero;
    private boolean carry;
    private boolean overflow;

    public void update(long result, boolean negative, boolean zero, boolean carry, boolean overflow) {
        this.negative = negative;
        this.zero = zero;
        this.carry = carry;
        this.overflow = overflow;
    }

    public void reset() {
        negative = false;
        zero = false;
        carry = false;
        overflow = false;
    }

    @Override
    public String toString() {
        return String.format("Flags: N=%b, Z=%b, C=%b, V=%b", negative, zero, carry, overflow);
    }
}