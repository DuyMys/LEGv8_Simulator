package instruction;

import java.util.BitSet;

/**
 * BFormatInstruction is a class that represents a branch format instruction in the LEGv8 architecture.
 * It extends the Instruction class and provides methods to disassemble the instruction and extract its components.
 */
public class BFormatInstruction extends Instruction {
    private final int offset;

    // --- Constructor ---
    /**
     * Constructor for BFormatInstruction.
     * @param bytecode The bytecode of the instruction as a BitSet.
     * @param definition The InstructionDefinition for this instruction.
     */
    public BFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.offset = getAddress_B();
    }

    // --- Instruction Methods ---
    /**
     * @return The instruction as assembled string.
     *         The string is formatted as "mnemonic #offset" (offset in instructions).
     */
    @Override
    public String disassemble() {
        String mnemonic = getDefinition().getMnemonic();
        // Offset in instructions
        int displayOffset = (offset & 0x2000000) != 0 ? (offset | 0xFC000000) : offset;
        return String.format("%-6s #%d", mnemonic, displayOffset);
    }

    // --- Getters ---
    @Override
    public int getAddress_B() {
        return extractBits(bytecode, 0, 25); // Bits 0-25
    }

    @Override
    public int getImmediate_I() {
        throw new UnsupportedOperationException("getImmediate_I not supported for B format");
    }
    /**
     * Calculates the branch target address for B-format instructions.
     * @param pc The current program counter (address of this instruction).
     * @return The branch target address.
     */
    public int getBranchAddress(int pc) {
        // Sign-extend the 26-bit offset
        int signedOffset = (offset << 6) >> 6; // Sign-extend from 26 bits
        // LEGv8 B-format: offset is in instructions, shift left by 2 to get byte offset
        return pc + (signedOffset << 2);
    }
}
