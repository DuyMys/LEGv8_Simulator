package instruction;

import java.util.BitSet;

/**
 * DFormatInstruction is a class that represents a data transfer format instruction in the LEGv8 architecture.
 * It extends the Instruction class and provides methods to disassemble the instruction and extract its components.
 */
public class DFormatInstruction extends Instruction {
    private final int rt, rn;
    private final int address;

    // --- Constructor ---
    /**
     * Constructor for DFormatInstruction.
     * @param bytecode The bytecode of the instruction as a BitSet.
     * @param definition The InstructionDefinition for this instruction.
     */
    public DFormatInstruction(BitSet bytecode, InstructionDefinition definition) {
        super(bytecode, definition);
        this.rt = getRt_D();
        this.rn = getRn_D();
        this.address = getAddress_D();
    }

    // --- Instruction Methods ---
    /**
     * @return The instruction as assembled string.
     *         The string is formatted as "mnemonic Xn, [Xn, #offset]".
     */
    @Override
    public String disassemble() {
        String mnemonic = getDefinition().getMnemonic();
        // Sign-extend address for display
        int displayAddr = (address & 0x100) != 0 ? (address | 0xFFFFFE00) : address;
        return String.format("%-6s X%d, [X%d, #%d]", mnemonic, rt, rn, displayAddr);
    }

    // --- Getters ---
    @Override
    public int getRt_D() {
        return extractBits(bytecode, 0, 4); // Bits 0-4
    }

    @Override
    public int getRn_D() {
        return extractBits(bytecode, 5, 9); // Bits 5-9
    }

    @Override
    public int getAddress_D() {
        return extractBits(bytecode, 12, 20); // Bits 12-20
    }

    @Override
    public int getImmediate_I() {
        return getAddress_D(); // Trả về address như immediate
    }
    /**
     * Determines if this D-format instruction is a load instruction.
     * @return true if the instruction is a load, false otherwise.
     */
    public boolean isLoad() {
        String mnemonic = getDefinition().getMnemonic().toUpperCase();
        // Common LEGv8 load mnemonics: LDUR, LDURB, LDURH, LDURSW, LDR, LDRB, LDRH, LDRSW
        return mnemonic.startsWith("L");
    }
}
