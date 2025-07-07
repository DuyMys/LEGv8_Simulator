package datapath;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the components and paths of the LEGv8 single-cycle datapath.
 */
public class Datapath {
    /**
     * Enum representing datapath components.
     */
    public enum Component {
        INSTRUCTION_MEMORY("Instruction Memory", "Stores instructions, provides instruction to registers and sign-extend."),
        REGISTERS("Registers", "Stores 32 64-bit registers (X0-X31), supports read and write operations."),
        MUX_ALUsrc("MUX_ALUsrc", "Selects between register data and sign-extended immediate for ALU input."),
        MUX_memtoreg("MUX_memtoreg", "Selects between ALU result and Data Memory read data for register write-back."),
        ALU("ALU", "Performs arithmetic and logical operations, outputs result and zero flag."),
        SIGN_EXTEND("Sign-extend", "Extends immediate fields to 64 bits for ALU or memory address calculation."),
        DATA_MEMORY("Data Memory", "Stores data, supports 64-bit read and write operations."),
        MUX_reg2loc("MUX_reg2loc", "Selects between register data and immediate value for write-back to registers."),
        PC("Program Counter", "Holds the address of the current instruction, updated after each instruction."),
        ADD_1("Adder 1", "Calculates the next instruction address by adding 4 to the current PC."),
        ADD_2("Adder 2", "Calculates branch target address by adding sign-extended immediate to PC."),
        SHIFT_LEFT_2("Shift Left 2", "Shifts immediate value left by 2 bits for word-aligned addresses."),
        MUX_PCSRC("MUX_PCSRC", "Selects between next PC from Adder 1 or branch target address from Adder 2."),
        CONTROL_UNIT("Control Unit", "Generates control signals based on the instruction opcode."),
        ALU_CONTROL("ALU Control", "Decodes ALU operation based on control signals and instruction type."),
        AND_GATE("AND Gate", "Performs logical AND operation, used for control signal generation."),
        OR_GATE("OR Gate", "Performs logical OR operation, used for control signal generation."),
        ADD_4("Adder 4", "Provides constant value 4 for PC increment, used in Adder 1."),
        AND2_GATE("AND2 Gate", "Performs logical AND operation for branch decision, used in control flow."),
        NFLAG("N Flag", "Negative flag, indicates if the result of the last ALU operation was negative."),
        ZFLAG("Z Flag", "Zero flag, indicates if the result of the last ALU operation was zero."),
        CFLAG("C Flag", "Carry flag, indicates if there was a carry out from the last ALU operation."),
        VFLAG("V Flag", "Overflow flag, indicates if the last ALU operation resulted in an overflow.");

        private final String name;
        private final String description;

        Component(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return name + ": " + description;
        }
    }

    /**
     * Class representing a path (connection) between two components.
     */
    public static class Path {
        private final Component source;
        private final String sourceOutput;
        private final Component destination;
        private final String destinationInput;
        private final String description;

        public Path(Component source, String sourceOutput, Component destination, 
                    String destinationInput, String description) {
            this.source = source;
            this.sourceOutput = sourceOutput;
            this.destination = destination;
            this.destinationInput = destinationInput;
            this.description = description;
        }

        public Component getSource() {
            return source;
        }

        public String getSourceOutput() {
            return sourceOutput;
        }

        public Component getDestination() {
            return destination;
        }

        public String getDestinationInput() {
            return destinationInput;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return String.format("%s (%s) -> %s (%s): %s", 
                source.getName(), sourceOutput, destination.getName(), destinationInput, description);
        }
    }

    // List of all components
    private final List<Component> components;

    // List of all paths
    private final List<Path> paths;

    /**
     * Constructs the LEGv8 datapath with predefined components and paths.
     */
    public Datapath() {
        // Initialize components
        components = new ArrayList<>();
        components.add(Component.INSTRUCTION_MEMORY);
        components.add(Component.REGISTERS);
        components.add(Component.MUX_ALUsrc);
        components.add(Component.MUX_memtoreg);
        components.add(Component.ALU);
        components.add(Component.SIGN_EXTEND);
        components.add(Component.DATA_MEMORY);
        components.add(Component.MUX_reg2loc);
        components.add(Component.PC);
        components.add(Component.ADD_1);
        components.add(Component.ADD_2);
        components.add(Component.SHIFT_LEFT_2);
        components.add(Component.MUX_PCSRC);
        components.add(Component.CONTROL_UNIT);
        components.add(Component.ALU_CONTROL);
        components.add(Component.AND_GATE);
        components.add(Component.OR_GATE);

        // Initialize paths
        // Register paths
        paths = new ArrayList<>();
        paths.add(new Path(
            Component.REGISTERS, "Read\nData 1",
            Component.ALU, "",
            "Carries first register value to ALU for computation."
        ));
        paths.add(new Path(
            Component.REGISTERS, "Read\nData 2",
            Component.MUX_ALUsrc, "0",
            "Carries second register value to MUX for potential ALU or memory input."
        ));
        paths.add(new Path(
            Component.REGISTERS, "",
            Component.DATA_MEMORY, "Write Data",
            "Sends second register value to Data Memory for write operations."
        ));
        // ALU paths
        paths.add(new Path(
            Component.ALU, "Zero",
            Component.AND2_GATE, "",
            "Sends ALU zero flag to AND2 gate for branch decision."
        ));
        paths.add(new Path(
            Component.ALU, "ALU\nResult",
            Component.DATA_MEMORY, "Address",
            "Provides computed address for memory read/write operations."
        ));
        paths.add(new Path(
            Component.ALU, "",
            Component.MUX_memtoreg, "0",
            "Sends ALU result to MUX for potential register write-back."
        ));
        paths.add(new Path(
            Component.ALU, "",
            Component.NFLAG, "",
            "Sends ALU negative flag to N Flag component."
        ));
        // Memory paths
        paths.add(new Path(
            Component.DATA_MEMORY, "Read\nData",
            Component.MUX_memtoreg, "1",
            "Sends memory read data to MUX for register write-back."
        ));
        paths.add(new Path(
            Component.MUX_memtoreg, "",
            Component.REGISTERS, "Write Data",
            "Sends selected data (ALU result or memory data) to registers for write-back."
        ));
        // Instruction memory paths
        paths.add(new Path(
            Component.INSTRUCTION_MEMORY, "Instruction\n[31-0]",
            Component.REGISTERS, "",
            "Provides register index for first read operand."
        ));
        paths.add(new Path(
            Component.INSTRUCTION_MEMORY, "Instruction [9-5]",
            Component.REGISTERS, "Read\nRegister 1",
            "Provides register index for first read operand."
        ));
        paths.add(new Path(
            Component.INSTRUCTION_MEMORY, "Instruction [20-16]",
            Component.MUX_reg2loc, "0",
            "Provides register index for second read operand (if applicable)."
        ));
        // paths.add(new Path(
        //     Component.INSTRUCTION_MEMORY, "Instruction",
        //     Component.REGISTERS, "Read\nRegister 2",
        //     "Provides register index for second read operand."
        // ));
        paths.add(new Path(
            Component.INSTRUCTION_MEMORY, "Instruction [4-0]",
            Component.REGISTERS, "Write\nRegister",
            "Provides register index for write-back."
        ));
        
        paths.add(new Path(
            Component.INSTRUCTION_MEMORY, "Instruction [31-21]",
            Component.CONTROL_UNIT, "",
            "Provides instruction to control unit for generating control signals."
        ));
        paths.add(new Path(
            Component.INSTRUCTION_MEMORY, "Instruction [31-0]",
            Component.SIGN_EXTEND, "",
            "Sends immediate field to sign-extend unit."
        ));
        paths.add(new Path(
            Component.INSTRUCTION_MEMORY, "Instruction [31-21] ",
            Component.ALU_CONTROL, "",
            "Sends instruction to ALU control unit for operation decoding."
        ));
        paths.add(new Path(
            Component.INSTRUCTION_MEMORY, "",
            Component.MUX_reg2loc, "1",
            "Sends instruction to MUX for selecting register or immediate for write-back."));

        // Sign extend paths
        paths.add(new Path(
            Component.SIGN_EXTEND, "",
            Component.MUX_ALUsrc, "1",
            "Sends sign-extended immediate to MUX for ALU or memory address."
        ));
        paths.add(new Path(
            Component.SIGN_EXTEND, "",
            Component.SHIFT_LEFT_2, "",
            "Sends sign-extended immediate to shift left unit for branch target calculation."
        ));
        paths.add(new Path(
            Component.MUX_ALUsrc, "",
            Component.ALU, "",
            "Sends selected data (register or immediate) to ALU for computation."
        ));
        // PC paths
        paths.add(new Path(
            Component.PC, "",
            Component.ADD_1, "",
            "Provides current PC value to the first adder for next instruction address."
        ));
        paths.add(new Path(
            Component.PC, "",
            Component.INSTRUCTION_MEMORY, "Read\nAddress",
            "Sends next PC value to Instruction Memory for fetching the next instruction."
        ));
        paths.add(new Path(
            Component.PC, "",
            Component.ADD_2, "",
            "Provides current PC value to the second adder for branch target address."
        ));
        paths.add(new Path(
            Component.ADD_1, "",
            Component.MUX_PCSRC, "0",
            "Sends next PC address to MUX for branch decision."
        ));
        paths.add(new Path(
            Component.ADD_4, "",
            Component.ADD_1, "",
            "Provides constant 4 to Adder 1 for PC increment."
        ));
        paths.add(new Path(
            Component.ADD_2, "ALU\nResult",
            Component.MUX_PCSRC, "1",
            "Sends branch target address to MUX for PC selection."
        ));
        paths.add(new Path(
            Component.MUX_PCSRC, "",
            Component.PC, "",
            "Selects next PC value (from Adder 1 or branch target) and updates PC."
        ));
        paths.add(new Path(
            Component.AND_GATE, "",
            Component.OR_GATE, "",
            "Sends AND gate output to OR gate for branch decision."
        ));
        paths.add(new Path(
            Component.OR_GATE, "",
            Component.MUX_PCSRC, "",
            "Sends OR gate output to MUX for unconditional branch decision."
        ));
        paths.add(new Path(
            Component.SHIFT_LEFT_2, "",
            Component.ADD_2, "",
            "Shifts immediate value left by 2 bits for word-aligned addresses."
        ));
        // Control unit paths
        paths.add(new Path(
            Component.CONTROL_UNIT, "ALUOp",
            Component.ALU_CONTROL, "",
            "Generates ALU control signals based on instruction type."
        ));
        paths.add(new Path(
            Component.CONTROL_UNIT, "ALUSrc",
            Component.MUX_ALUsrc, "",
            "Generates ALU source select signal for MUX input."
        ));
        paths.add(new Path(
            Component.CONTROL_UNIT, "MemRead",
            Component.DATA_MEMORY, "",
            "Generates control signals for memory read operations."
        ));
        paths.add(new Path(
            Component.CONTROL_UNIT, "MemWrite",
            Component.DATA_MEMORY, "",
            "Generates control signals for memory write operations."
        ));
        paths.add(new Path(
            Component.CONTROL_UNIT, "MemtoReg",
            Component.MUX_memtoreg, "",
            "Generates select signal for MUX to choose between ALU result or memory data."
        ));
        paths.add(new Path(
            Component.CONTROL_UNIT, "RegWrite",
            Component.REGISTERS, "",
            "Generates control signal for register write operation."
        ));
        paths.add(new Path(
            Component.CONTROL_UNIT, "Reg2Loc",
            Component.MUX_reg2loc, "",
            "Generates select signal for MUX to choose between register or immediate for write-back."
        ));
        paths.add(new Path(
            Component.CONTROL_UNIT, "ZeroBranch",
            Component.AND2_GATE, "",
            "Generates control signal for branch operations using AND gate."
        ));
        paths.add(new Path(
            Component.CONTROL_UNIT, "FlagBranch",
            Component.AND_GATE, "",
            "Generates control signal for branch operations based on ALU flags using AND gate."
        ));
        paths.add(new Path(
            Component.CONTROL_UNIT, "FlagWrite",
            Component.NFLAG, "",
            "Generates control signal for write operations based on ALU flags using AND gate."
        ));
        paths.add(new Path(
            Component.CONTROL_UNIT, "Uncondbranch",
            Component.OR_GATE, "",
            "Generates control signal for unconditional branch operations using OR gate."
        ));
        paths.add(new Path(
            Component.ALU_CONTROL, "",
            Component.ALU, "",
            "Sends decoded ALU operation to ALU for execution."
        ));
        paths.add(new Path(
            Component.MUX_reg2loc, "",
            Component.REGISTERS, "Read\nRegister 2",
            "Sends selected data (register or immediate) to registers for write-back."
        ));
        paths.add(new Path(
            Component.AND2_GATE, "",
            Component.OR_GATE, "",
            "Sends AND2 gate output to OR gate for branch decision."
        ));
        paths.add(new Path(
            Component.NFLAG, "",
            Component.AND_GATE, "",
            "Sends NF flag output to AND gate for branch decision."
        ));
    }

    /**
     * Returns the list of datapath components.
     * @return List of components.
     */
    public List<Component> getComponents() {
        return new ArrayList<>(components);
    }

    /**
     * Returns the list of datapath paths.
     * @return List of paths.
     */
    public List<Path> getPaths() {
        return new ArrayList<>(paths);
    }

    /**
     * Returns a string representation of the datapath.
     * @return String describing components and paths.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LEGv8 Datapath:\n\nComponents:\n");
        for (Component comp : components) {
            sb.append(comp).append("\n");
        }
        sb.append("\nPaths:\n");
        for (Path path : paths) {
            sb.append(path).append("\n");
        }
        return sb.toString();
    }
}
