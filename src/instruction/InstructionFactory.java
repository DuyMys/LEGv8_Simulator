package instruction;

import util.*;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        int opcode10 = Instruction.extractBits(bytecode, 22, 31);
        int opcode9 = Instruction.extractBits(bytecode, 23, 31);
        int opcode8 = Instruction.extractBits(bytecode, 24, 31);
        int opcode6 = Instruction.extractBits(bytecode, 26, 31);

        InstructionDefinition definition = null;
        char format = '?';

        if ((definition = configLoader.getDefinition(opcode11, 'R')) != null) {
            format = 'R';
        } else if ((definition = configLoader.getDefinition(opcode11, 'D')) != null) {
            format = 'D';
        } else if ((definition = configLoader.getDefinition(opcode10, 'I')) != null) {
            format = 'I';
        } else if ((definition = configLoader.getDefinition(opcode9, 'M')) != null) {
            format = 'M';
        } else if ((definition = configLoader.getDefinition(opcode8, 'C')) != null) {
            format = 'C';
        } else if ((definition = configLoader.getDefinition(opcode6, 'B')) != null) {
            format = 'B';
        }

        if (definition == null) {
            System.err.printf("%sNo instruction definition found for bytecode: %s\n",
                    ColoredLog.WARNING, Instruction.formatBitSet(bytecode));
            return null;
        }

        switch (format) {
            case 'R':
                return new RFormatInstruction(bytecode, definition);
            case 'I':
                return new IFormatInstruction(bytecode, definition);
            case 'D':
                return new DFormatInstruction(bytecode, definition);
            case 'M':
                return new IMFormatInstruction(bytecode, definition);
            case 'B':
                return new BFormatInstruction(bytecode, definition);
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

        // Normalize commas, spaces, and brackets
        assemblyLine = assemblyLine.trim()
                .replaceAll("\\s*,\\s*", ",")
                .replaceAll("\\s*\\[\\s*", "[")
                .replaceAll("\\s*\\]\\s*", "]");

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
        String[] parts = new String[4];
        parts[0] = mnemonic;

        switch (definition.getFormat()) {
            case 'R':
                // Handle CMP specially (only 2 operands)
                if (mnemonic.equals("CMP")) {
                    String[] rParts = operands.split(",");
                    if (rParts.length != 2) {
                        System.err.printf("%sInvalid CMP instruction format: %s\n", ColoredLog.WARNING, assemblyLine);
                        return null;
                    }
                    parts[1] = "XZR"; // CMP doesn't write to a register
                    parts[2] = rParts[0].trim();
                    parts[3] = rParts[1].trim();
                } else {
                    // ADD, SUB, AND, ORR: "Xd, Xn, Xm"
                    String[] rParts = operands.split(",");
                    if (rParts.length != 3) {
                        System.err.printf("%sInvalid R-format instruction format: %s\n", ColoredLog.WARNING, assemblyLine);
                        return null;
                    }
                    for (int i = 0; i < 3; i++) {
                        parts[i + 1] = rParts[i].trim();
                    }
                }
                break;

            case 'I':
                // ADDI, SUBI: "Xd, Xn, #imm"
                String[] iParts = operands.split(",");
                if (iParts.length != 3 || !iParts[2].trim().startsWith("#")) {
                    System.err.printf("%sInvalid I-format instruction format: %s\n", ColoredLog.WARNING, assemblyLine);
                    return null;
                }
                for (int i = 0; i < 3; i++) {
                    parts[i + 1] = iParts[i].trim();
                }
                break;

            case 'D':
                // LDUR, STUR: "Xt, [Xn, #offset]"
                Pattern dPattern = Pattern.compile("(\\w+)\\s*,\\s*\\[(\\w+)\\s*,\\s*(#[\\d-]+)\\]");
                Matcher dMatcher = dPattern.matcher(operands);
                if (dMatcher.matches()) {
                    parts[1] = dMatcher.group(1); // Xt
                    parts[2] = dMatcher.group(2); // Xn
                    parts[3] = dMatcher.group(3); // #offset
                } else {
                    System.err.printf("%sInvalid D-format instruction format: %s\n", ColoredLog.WARNING, assemblyLine);
                    return null;
                }
                break;

            case 'M':
                // MOVZ: "Xd, #imm, LSL #shift" or MOV: "Xd, #imm"
                if (mnemonic.equals("MOV")) {
                    // Handle simpler MOV syntax: "MOV Xd, #imm"
                    String[] movParts = operands.split(",");
                    if (movParts.length != 2 || !movParts[1].trim().startsWith("#")) {
                        System.err.printf("%sInvalid MOV instruction format: %s\n", ColoredLog.WARNING, assemblyLine);
                        return null;
                    }
                    parts[1] = movParts[0].trim(); // Xd
                    parts[2] = movParts[1].trim(); // #imm
                    parts[3] = "LSL #0"; // Default shift of 0
                } else {
                    // MOVZ: "Xd, #imm, LSL #shift"
                    Pattern mPattern = Pattern.compile("(\\w+)\\s*,\\s*(#[\\d]+)\\s*,\\s*(LSL\\s*#[\\d]+)");
                    Matcher mMatcher = mPattern.matcher(operands);
                    if (mMatcher.matches()) {
                        parts[1] = mMatcher.group(1); // Xd
                        parts[2] = mMatcher.group(2); // #imm
                        parts[3] = mMatcher.group(3); // LSL #shift
                    } else {
                        System.err.printf("%sInvalid IM-format instruction format: %s\n", ColoredLog.WARNING, assemblyLine);
                        return null;
                    }
                }
                break;

            case 'B':
                // B: "#offset" or "label"
                if (operands.startsWith("#")) {
                    // Direct numeric offset: B #4
                    parts[1] = operands.trim(); // #offset
                } else {
                    // Label: B skip, B done
                    // For now, treat labels as zero offset (will need label resolution later)
                    parts[1] = "#0"; // Placeholder - labels need to be resolved later
                    // Store the label for potential future resolution
                    // Note: Full label resolution would require a two-pass assembler
                }
                break;

            default:
                System.err.printf("%sUnsupported instruction format: %c\n", ColoredLog.WARNING, definition.getFormat());
                return null;
        }

        System.out.println("Parts: " + java.util.Arrays.toString(parts));

        BitSet bytecode = new BitSet(32);
        switch (definition.getFormat()) {
            case 'R':
                return assembleRFormat(parts, definition, bytecode);
            case 'I':
                return assembleIFormat(parts, definition, bytecode);
            case 'D':
                return assembleDFormat(parts, definition, bytecode);
            case 'M':
                return assembleIMFormat(parts, definition, bytecode);
            case 'B':
                return assembleBFormat(parts, definition, bytecode);
            default:
                System.err.printf("%sUnsupported instruction format: %c\n", ColoredLog.WARNING, definition.getFormat());
                return null;
        }
    }

    // Label resolution support
    private Map<String, Integer> labelMap = new HashMap<>();

    /**
     * Create instructions from multiple assembly lines with label resolution
     */
    public List<Instruction> createFromAssemblyLines(String[] assemblyLines) {
        labelMap.clear();
        List<String> cleanedLines = new java.util.ArrayList<>();

        // First pass: collect labels and clean up lines
        int instructionIndex = 0; // Index for actual instructions (excluding labels)
        for (String line : assemblyLines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith(";")) {
                continue; // Skip empty lines and comments
            }

            // Check for labels (lines ending with colon)
            if (line.endsWith(":")) {
                String label = line.substring(0, line.length() - 1).trim();
                labelMap.put(label, instructionIndex); // Use instruction index, not line index
                System.out.println("Found label: " + label + " at instruction index " + instructionIndex);
                continue; // Don't add label lines to instruction list
            }

            cleanedLines.add(line);
            instructionIndex++; // Increment only for actual instructions
        }

        // Second pass: create instructions with resolved labels
        List<Instruction> instructions = new java.util.ArrayList<>();
        for (int i = 0; i < cleanedLines.size(); i++) {
            String line = cleanedLines.get(i);
            Instruction instruction = createFromAssemblyWithLabels(line, i);
            if (instruction != null) {
                instructions.add(instruction);
            }
        }

        return instructions;
    }

    /**
     * Create instruction from assembly line with label resolution
     */
    private Instruction createFromAssemblyWithLabels(String assemblyLine, int currentLineIndex) {
        // Handle B-format instructions with labels
        if (assemblyLine.trim().toUpperCase().startsWith("B ")) {
            String operand = assemblyLine.substring(2).trim();
            if (!operand.startsWith("#")) {
                // This is a label reference
                String label = operand;
                if (labelMap.containsKey(label)) {
                    int targetLine = labelMap.get(label);
                    int offset = targetLine - currentLineIndex;
                    // Replace the label with the calculated offset
                    assemblyLine = "B #" + offset;
                    System.out.println("Resolved label '" + label + "' to offset " + offset);
                } else {
                    System.err.printf("%sUndefined label: %s\n", ColoredLog.WARNING, label);
                    return null;
                }
            }
        }

        // Use the original createFromAssembly method
        return createFromAssembly(assemblyLine);
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

    private Instruction assembleIFormat(String[] parts, InstructionDefinition definition, BitSet bytecode) {
        if (parts.length != 4 || !parts[3].startsWith("#")) {
            System.err.printf("%sInvalid I-format instruction format: %s\n", ColoredLog.WARNING, String.join(" ", parts));
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
            int rn = parseRegister(parts[2]);
            int immediate = Integer.parseInt(parts[3].substring(1));

            if (immediate < -2048 || immediate > 2047) {
                throw new IllegalArgumentException("Immediate value out of range (12-bit signed): " + immediate);
            }

            Instruction.setBits(bytecode, rd, 0, 4);
            Instruction.setBits(bytecode, rn, 5, 9);
            Instruction.setBits(bytecode, immediate & 0xFFF, 10, 21);

            return new IFormatInstruction(bytecode, definition);
        } catch (IllegalArgumentException e) {
            System.err.printf("%sError assembling I-format instruction: %s\n", ColoredLog.WARNING, e.getMessage());
            return null;
        }
    }

    private Instruction assembleDFormat(String[] parts, InstructionDefinition definition, BitSet bytecode) {
        if (parts.length != 4 || !parts[3].startsWith("#")) {
            System.err.printf("%sInvalid D-format instruction format: %s\n", ColoredLog.WARNING, String.join(" ", parts));
            return null;
        }

        try {
            String opcode = definition.getOpcodeId();
            for (int i = 0; i < opcode.length(); i++) {
                if (opcode.charAt(opcode.length() - 1 - i) == '1') {
                    bytecode.set(31 - i);
                }
            }

            int rt = parseRegister(parts[1]);
            int rn = parseRegister(parts[2]);
            int address = Integer.parseInt(parts[3].substring(1));

            if (address < -256 || address > 255) {
                throw new IllegalArgumentException("Address offset out of range (9-bit signed): " + address);
            }

            Instruction.setBits(bytecode, rt, 0, 4);
            Instruction.setBits(bytecode, rn, 5, 9);
            Instruction.setBits(bytecode, address & 0x1FF, 12, 20);
            Instruction.setBits(bytecode, 0, 10, 11); // op2 = 00 for LDUR/STUR

            return new DFormatInstruction(bytecode, definition);
        } catch (IllegalArgumentException e) {
            System.err.printf("%sError assembling D-format instruction: %s\n", ColoredLog.WARNING, e.getMessage());
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

    private Instruction assembleBFormat(String[] parts, InstructionDefinition definition, BitSet bytecode) {
        if (parts.length != 2 || !parts[1].startsWith("#")) {
            System.err.printf("%sInvalid B-format instruction format: %s\n", ColoredLog.WARNING, String.join(" ", parts));
            return null;
        }

        try {
            String opcode = definition.getOpcodeId();
            for (int i = 0; i < opcode.length(); i++) {
                if (opcode.charAt(opcode.length() - 1 - i) == '1') {
                    bytecode.set(31 - i);
                }
            }

            int offset = Integer.parseInt(parts[1].substring(1));

            if (offset < -33554432 || offset > 33554431) {
                throw new IllegalArgumentException("Branch offset out of range (26-bit signed): " + offset);
            }

            Instruction.setBits(bytecode, offset & 0x3FFFFFF, 0, 25);

            return new BFormatInstruction(bytecode, definition);
        } catch (IllegalArgumentException e) {
            System.err.printf("%sError assembling B-format instruction: %s\n", ColoredLog.WARNING, e.getMessage());
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