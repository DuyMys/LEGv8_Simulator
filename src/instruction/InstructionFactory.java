package instruction;
import util.*;

import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factory class for creating Instruction objects from bytecode or assembly lines.
 */
public class InstructionFactory {
    private final InstructionConfigLoader configLoader;

    public InstructionFactory(InstructionConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public Instruction createFromBytecode(BitSet bytecode) {
        int opcode11 = Instruction.extractBits(bytecode, 21, 31);
        int opcode9 = Instruction.extractBits(bytecode, 23, 31);
        int opcode8 = Instruction.extractBits(bytecode, 24, 31);
        int opcode6 = Instruction.extractBits(bytecode, 26, 31);
        int opcode10 = Instruction.extractBits(bytecode, 22, 31);

        InstructionDefinition definition = null;
        char format = '?';

        if ((definition = configLoader.getDefinition(opcode11, 'R')) != null) {
            format = 'R';
        } else if ((definition = configLoader.getDefinition(opcode11, 'D')) != null) {
            format = 'D';
        } else if ((definition = configLoader.getDefinition(opcode9, 'M')) != null) {
            format = 'M';
        } else if ((definition = configLoader.getDefinition(opcode8, 'C')) != null) {
            format = 'C';
        } else if ((definition = configLoader.getDefinition(opcode6, 'B')) != null) {
            format = 'B';
        } else if ((definition = configLoader.getDefinition(opcode10, 'I')) != null) {
            format = 'I';
        }

        if (definition == null) {
            System.err.printf("%sNo instruction definition found for bytecode: %s\n",
                    ColoredLog.WARNING, Instruction.formatBitSet(bytecode));
            return null;
        }

        switch (format) {
            case 'R':
                return new RFormatInstruction(bytecode, definition);
            case 'M':
                return new IMFormatInstruction(bytecode, definition);
            default:
                System.err.printf("%sUnsupported instruction format: %c\n", ColoredLog.WARNING, format);
                return null;
        }
    }

    public Instruction createFromAssembly(String assemblyLine) {
        if (assemblyLine == null || assemblyLine.trim().isEmpty()) {
            System.err.printf("%sInvalid assembly line: %s\n", ColoredLog.WARNING, assemblyLine);
            return null;
        }

        // Normalize commas and spaces
        assemblyLine = assemblyLine.trim().replaceAll("\\s*,\\s*", ",");

        // Extract mnemonic
        int firstSpace = assemblyLine.indexOf(' ');
        if (firstSpace == -1) {
            System.err.printf("%sInvalid assembly line format: %s\n", ColoredLog.WARNING, assemblyLine);
            return null;
        }
        String mnemonic = assemblyLine.substring(0, firstSpace).toUpperCase();
        InstructionDefinition definition = configLoader.getDefinitionByMnemonic(mnemonic);
        if (definition == null) {
            System.err.printf("%sUnknown mnemonic: %s\n", ColoredLog.WARNING, mnemonic);
            return null;
        }

        // Split remaining parts based on format
        String operands = assemblyLine.substring(firstSpace + 1).trim();
        String[] parts;

        if (definition.getFormat() == 'M') {
            // MOVZ: Expect "Xd, #imm, LSL #shift"
            Pattern pattern = Pattern.compile("(\\w+)\\s*,\\s*(#[\\d]+)\\s*,\\s*(LSL\\s*#[\\d]+)");
            Matcher matcher = pattern.matcher(operands);
            if (matcher.matches()) {
                parts = new String[4];
                parts[0] = mnemonic;
                parts[1] = matcher.group(1); // Xd
                parts[2] = matcher.group(2); // #imm
                parts[3] = matcher.group(3); // LSL #shift
            } else {
                System.err.printf("%sInvalid IM-format instruction format: %s\n", ColoredLog.WARNING, assemblyLine);
                return null;
            }
        } else {
            // R-format (e.g., ADD): Expect "Xd, Xn, Xm"
            parts = new String[4];
            parts[0] = mnemonic;
            String[] operandParts = operands.split(",");
            if (operandParts.length != 3) {
                System.err.printf("%sInvalid R-format instruction format: %s\n", ColoredLog.WARNING, assemblyLine);
                return null;
            }
            for (int i = 0; i < 3; i++) {
                parts[i + 1] = operandParts[i].trim();
            }
        }

        System.out.println("Parts: " + java.util.Arrays.toString(parts));

        BitSet bytecode = new BitSet(32);
        switch (definition.getFormat()) {
            case 'R':
                return assembleRFormat(parts, definition, bytecode);
            case 'M':
                return assembleIMFormat(parts, definition, bytecode);
            default:
                System.err.printf("%sUnsupported instruction format: %c\n", ColoredLog.WARNING, definition.getFormat());
                return null;
        }
    }

    private Instruction assembleRFormat(String[] parts, InstructionDefinition definition, BitSet bytecode) {
        String mnemonic = definition.getMnemonic();
        int rd = 0, rn = 0, rm = 0, shamt = 0;

        try {
            String opcode = definition.getOpcodeId();
            for (int i = 0; i < opcode.length(); i++) {
                if (opcode.charAt(opcode.length() - 1 - i) == '1') {
                    bytecode.set(31 - i);
                }
            }

            if (mnemonic.equals("BR")) {
                if (parts.length != 2) {
                    System.err.printf("%sInvalid BR format: %s\n", ColoredLog.WARNING, String.join(" ", parts));
                    return null;
                }
                rn = parseRegister(parts[1]);
                Instruction.setBits(bytecode, rn, 5, 9);
            } else if (mnemonic.equals("LSL") || mnemonic.equals("LSR") || mnemonic.equals("ASR")) {
                if (parts.length != 4 || !parts[3].startsWith("#")) {
                    System.err.printf("%sInvalid shift instruction format: %s\n", ColoredLog.WARNING, String.join(" ", parts));
                    return null;
                }
                rd = parseRegister(parts[1]);
                rn = parseRegister(parts[2]);
                shamt = Integer.parseInt(parts[3].substring(1));
                Instruction.setBits(bytecode, rd, 0, 4);
                Instruction.setBits(bytecode, rn, 5, 9);
                Instruction.setBits(bytecode, shamt, 10, 15);
                Instruction.setBits(bytecode, parseRegister(parts[2]), 16, 20);
            } else {
                if (parts.length != 4) {
                    System.err.printf("%sInvalid R-format instruction format: %s\n", ColoredLog.WARNING, String.join(" ", parts));
                    return null;
                }
                rd = parseRegister(parts[1]);
                rn = parseRegister(parts[2]);
                rm = parseRegister(parts[3]);
                Instruction.setBits(bytecode, rd, 0, 4);
                Instruction.setBits(bytecode, rn, 5, 9);
                Instruction.setBits(bytecode, rm, 16, 20);
            }

            return new RFormatInstruction(bytecode, definition);
        } catch (IllegalArgumentException e) {
            System.err.printf("%sError assembling R-format instruction: %s\n", ColoredLog.WARNING, e.getMessage());
            return null;
        }
    }

    private Instruction assembleIMFormat(String[] parts, InstructionDefinition definition, BitSet bytecode) {
        if (parts.length != 4 || !parts[2].startsWith("#") || !parts[3].toUpperCase().startsWith("LSL")) {
            System.err.printf("%sInvalid IM-format instruction format: %s\n", ColoredLog.WARNING, String.join(" ", parts));
            return null;
        }

        try {
            String opcode = definition.getOpcodeId();
            for (int i = 0; i < opcode.length(); i++) {
                if (opcode.charAt(opcode.length() - 1 - i) == '1') {
                    bytecode.set(31 - i);
                }
            }

            int rd = parseRegister(parts[1]);
            int immediate = Integer.parseInt(parts[2].substring(1));
            String shiftStr = parts[3].substring(parts[3].toUpperCase().indexOf('#') + 1);
            int shift = Integer.parseInt(shiftStr) / 16;

            if (immediate < 0 || immediate > 0xFFFF) {
                throw new IllegalArgumentException("Immediate value out of range: " + immediate);
            }
            if (shift < 0 || shift > 3) {
                throw new IllegalArgumentException("Invalid shift value: " + shiftStr);
            }

            Instruction.setBits(bytecode, rd, 0, 4);
            Instruction.setBits(bytecode, immediate, 5, 20);
            Instruction.setBits(bytecode, shift, 21, 22);

            return new IMFormatInstruction(bytecode, definition);
        } catch (IllegalArgumentException e) {
            System.err.printf("%sError assembling IM-format instruction: %s\n", ColoredLog.WARNING, e.getMessage());
            return null;
        }
    }

    private int parseRegister(String reg) {
        if (reg == null || reg.isEmpty()) {
            throw new IllegalArgumentException("Register cannot be null or empty");
        }
        reg = reg.replaceAll("[,\\s]+", "");
        if (!reg.matches("X\\d+|XZR")) {
            throw new IllegalArgumentException("Invalid register: " + reg);
        }
        if (reg.equals("XZR")) {
            return 31;
        }
        int regNum = Integer.parseInt(reg.substring(1));
        if (regNum < 0 || regNum > 31) {
            throw new IllegalArgumentException("Register index out of range: " + reg);
        }
        return regNum;
    }
}