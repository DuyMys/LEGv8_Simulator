package instruction;

import java.util.BitSet;

/**
 * IFormatInstruction is a class that represents an immediate format instruction in the LEGv8 architecture.
 * It extends the Instruction class and provides methods to disassemble the instruction and extract its components.
 */
public class IFormatInstruction extends Instruction {
    private final int rd, rn;
    private final int immediate;

    // --- Constructor ---
    /**
     * Constructor for IFormatInstruction.
     * @param bytecode The bytecode of the instruction as a BitSet.
     * @param definition The InstructionDefinition for this instruction.
     */
    public IFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.rd = getRd_I();
        this.rn = getRn_I();
        this.immediate = getImmediate_I();
    }

    // --- Instruction Methods ---
    /**
     * @return The instruction as assembled string.
     *         The string is formatted as "mnemonic Xn, Xn, #immediate".
     */
    @Override
    public String disassemble() {
        String mnemonic = getDefinition().getMnemonic();
        // Sign-extend immediate for display
        int displayImm = (immediate & 0x800) != 0 ? (immediate | 0xFFFFF000) : immediate;
        return String.format("%-6s X%d, X%d, #%d", mnemonic, rd, rn, displayImm);
    }

    // --- Getters ---
    @Override
    public int getRd_I() {
        return extractBits(bytecode, 0, 4); // Bits 0-4
    }

    @Override
    public int getRn_I() {
        return extractBits(bytecode, 5, 9); // Bits 5-9
    }

    @Override
    public int getImmediate_I() {
        return extractBits(bytecode, 10, 21); // Bits 10-21
    }
}