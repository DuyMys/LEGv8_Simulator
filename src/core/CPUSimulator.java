package core;
import util.*;
import datapath.*;
import instruction.*; 
import memory.Memory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.BitSet;

/**
 * Simulates a LEGv8 CPU, enhanced with datapath tracking for GUI visualization.
 */
public class CPUSimulator {
    // --- Core Components ---
    private final InstructionFactory factory;
    private final RegisterFileController registerFile;
    private final ArithmeticLogicUnit alu;
    private final ControlUnit controlUnit;
    private final Memory memory;
    private final List<Instruction> program;
    private int pc; 

    // --- CPU Flags ---
    private boolean zeroFlag;
    private boolean negativeFlag;
    private boolean overflowFlag;
    private boolean carryFlag;

    // --- Micro-Step Execution State ---
    private List<MicroStep> microStepQueue;
    private int currentMicroStepIndex;

    // --- State for GUI Visualization ---
    private List<String> activeComponents;
    private List<String> activeBuses;
    private Map<String, String> busDataValues;
    private String lastExecutedInstruction = "None";
    private boolean isFinished = false;

    public CPUSimulator(InstructionConfigLoader configLoader) {
        this.factory = new InstructionFactory(configLoader);
        this.registerFile = new RegisterFileController(new RegisterStorage());
        this.alu = new ArithmeticLogicUnit();
        this.controlUnit = new ControlUnit(configLoader);
        this.memory = new Memory();
        this.program = new ArrayList<>();
        this.microStepQueue = new ArrayList<>();
        
        // Initialize visualization state
        this.activeComponents = new ArrayList<>();
        this.activeBuses = new ArrayList<>();
        this.busDataValues = new HashMap<>();
        
        this.pc = 0;
    }

    public void loadProgram(String[] assemblyLines) {
        program.clear();
        pc = 0;
        for (String line : assemblyLines) {
            Instruction instruction = factory.createFromAssembly(line);
            if (instruction != null) {
                BitSet bytecode = instruction.getBytecode();
                int instructionCode = 0;
                for (int i = 0; i < 32; i++) {
                    if (bytecode.get(i)) {
                        instructionCode |= (1 << i);
                    }
                }
                System.out.printf("Loading instruction: %s -> Bytecode: 0x%08X\n", line, instructionCode);
                program.add(instruction);
            } else {
                System.err.println("Failed to load instruction: " + line);
            }
        }
        System.out.println("Program loaded with " + program.size() + " instruction(s).");
    }

    // public void executeProgram() {
    //     while (!isFinished) {
    //         step();
    //     }
    // }
    public void executeProgram() {
        while (pc < program.size()) {
            step();
        }
        System.out.println("Program finished.");
        printState();
    }

    private void executeInstruction(Instruction instruction) {
        ControlSignals signals = controlUnit.generateControlSignals(instruction);
        String mnemonic = instruction.getDefinition().getMnemonic();

        if (instruction instanceof RFormatInstruction) {
            RFormatInstruction rInst = (RFormatInstruction) instruction;
            long rnValue = registerFile.readRegister(rInst.getRn_R());
            long rmValue = registerFile.readRegister(signals.isReg2Loc() ? rInst.getRd_R() : rInst.getRm_R());
            int shift = rInst.getShift(); // Lấy giá trị shift từ RFormatInstruction
            ArithmeticLogicUnit.ALUResult result = null;

            // Xác định operation dựa trên mnemonic
            if (mnemonic.equals("ADD")) {
                result = alu.execute(rnValue, rmValue, signals.getAluOp(), 2); // ADD
            } else if (mnemonic.equals("SUB")) {
                result = alu.execute(rnValue, rmValue, signals.getAluOp(), 3); // SUB
            } else if (mnemonic.equals("AND")) {
                result = alu.execute(rnValue, rmValue, signals.getAluOp(), 0); // AND
            } else if (mnemonic.equals("ORR")) {
                result = alu.execute(rnValue, rmValue, signals.getAluOp(), 1); // ORR
            } else if (mnemonic.equals("EOR")) {
                result = alu.execute(rnValue, rmValue, signals.getAluOp(), 4); // EOR
            } else if (mnemonic.equals("MUL")) {
                result = alu.execute(rnValue, rmValue, signals.getAluOp(), 5); // MUL
            } else if (mnemonic.equals("SDIV")) {
                result = alu.execute(rnValue, rmValue, signals.getAluOp(), 7); // SDIV
            } else if (mnemonic.equals("UDIV")) {
                result = alu.execute(rnValue, rmValue, signals.getAluOp(), 8); // UDIV
            } else if (mnemonic.equals("LSL")) {
                result = alu.execute(rnValue, shift, signals.getAluOp(), 9); // LSL, dùng shift làm b
            } else if (mnemonic.equals("LSR")) {
                result = alu.execute(rnValue, shift, signals.getAluOp(), 10); // LSR
            } else if (mnemonic.equals("ASR")) {
                result = alu.execute(rnValue, shift, signals.getAluOp(), 11); // ASR
            } else if (mnemonic.equals("CMP")) {
                result = alu.execute(rnValue, rmValue, signals.getAluOp(), 6); // CMP
                // Không cần setRegWrite(false) vì regWrite đã được định nghĩa trong ControlUnit
            } else if (mnemonic.equals("SMULH")) {
                result = alu.execute(rnValue, rmValue, signals.getAluOp(), 12); // SMULH
            } else if (mnemonic.equals("UMULH")) {
                result = alu.execute(rnValue, rmValue, signals.getAluOp(), 13); // UMULH
            }

            if (signals.isRegWrite() && result != null) {
                registerFile.writeRegister(rInst.getRd_R(), result.result, true);
            }
            // Cập nhật cờ chỉ khi FlagW = 1
            if (signals.isFlagWrite() && result != null) {
                updateFlags(result);
            }
            String output = signals.isRegWrite() ? "X" + rInst.getRd_R() + "=" + result.result : "Flags updated";
            System.out.printf("%s -> %s\n", instruction.disassemble(), output);
        } else if (instruction instanceof IMFormatInstruction) {
            IMFormatInstruction imInst = (IMFormatInstruction) instruction;
            long immediate = imInst.getImmediate_IM() << (imInst.getShift_IM() * 16);
            long rnValue = signals.isAluSrc() ? 0 : registerFile.readRegister(imInst.getRd_IM());
            ArithmeticLogicUnit.ALUResult result = alu.execute(rnValue, immediate, signals.getAluOp(),
                    signals.getOperation());
            if (signals.isRegWrite()) {
                registerFile.writeRegister(imInst.getRd_IM(), result.result, true);
            }
            if (signals.isFlagWrite()) {
                updateFlags(result);
            }
            System.out.printf("%s -> X%d=%d\n", instruction.disassemble(), imInst.getRd_IM(), result.result);
        } else if (instruction instanceof IFormatInstruction) {
            IFormatInstruction iInst = (IFormatInstruction) instruction;
            long rnValue = registerFile.readRegister(iInst.getRn_I());
            long immediate = iInst.getImmediate_I();
            ArithmeticLogicUnit.ALUResult result = alu.execute(rnValue, immediate, signals.getAluOp(),
                    signals.getOperation());
            if (signals.isRegWrite()) {
                registerFile.writeRegister(iInst.getRd_I(), result.result, true);
            }
            if (signals.isFlagWrite()) {
                updateFlags(result);
            }
            System.out.printf("%s -> X%d=%d\n", instruction.disassemble(), iInst.getRd_I(), result.result);
        } else if (instruction instanceof DFormatInstruction) {
            DFormatInstruction dInst = (DFormatInstruction) instruction;
            long rnValue = registerFile.readRegister(dInst.getRn_D());
            long address = rnValue + dInst.getAddress_D();
            if (mnemonic.equals("LDUR")) {
                long value = memory.read(address, 8);
                if (signals.isRegWrite()) {
                    registerFile.writeRegister(dInst.getRt_D(), value, true);
                }
                System.out.printf("%s -> X%d=%d\n", instruction.disassemble(), dInst.getRt_D(), value);
            } else if (mnemonic.equals("STUR")) {
                long value = registerFile.readRegister(dInst.getRt_D());
                memory.write(address, value, 8);
                System.out.printf("%s -> [0x%X]=%d\n", instruction.disassemble(), address, value);
            }
            // Không cập nhật cờ trạng thái cho LDUR/STUR
        } else if (instruction instanceof BFormatInstruction) {
            BFormatInstruction bInst = (BFormatInstruction) instruction;
            long offset = bInst.getAddress_B();
            pc = pc + (int) offset - 1; // -1 vì pc++ trong step()
            System.out.printf("%s -> PC=%d\n", instruction.disassemble(), pc + 1);
            // Không cập nhật cờ trạng thái cho B
        }
    }

    // Phương thức cập nhật cờ trạng thái
    private void updateFlags(ArithmeticLogicUnit.ALUResult result) {
        zeroFlag = result.zero;
        negativeFlag = result.negative;
        overflowFlag = result.overflow;
        carryFlag = result.carry;
    }

    public void printState() {
        System.out.println("CPU State:");
        System.out.println("PC: " + pc);
        System.out.println("Registers:");
        for (int i = 0; i < 32; i++) {
            System.out.printf("X%-2d: 0x%016X  ", i, registerFile.readRegister(i));
            if ((i + 1) % 4 == 0)
                System.out.println();
        }
        System.out.println("Flags: ZF=" + (zeroFlag ? 1 : 0) + ", NF=" + (negativeFlag ? 1 : 0) +
                ", OF=" + (overflowFlag ? 1 : 0) + ", CF=" + (carryFlag ? 1 : 0));
    }

    /**
     * Executes ONE micro-step.
     */
    public void step() {
        if (isFinished) return;

        // If the queue is empty, it means we are at the start of a new instruction.
        // Generate the micro-steps for it.
        if (microStepQueue.isEmpty()) {
            if (pc >= program.size()) {
                isFinished = true;
                lastExecutedInstruction = "Execution Complete";
                clearDatapathActivity();
                return;
            }
            Instruction instruction = program.get(pc);
            lastExecutedInstruction = instruction.disassemble();
            generateMicroStepsFor(instruction);
        }
        
        // Execute the next micro-step in the queue
        if (currentMicroStepIndex < microStepQueue.size()) {
            MicroStep step = microStepQueue.get(currentMicroStepIndex);
            
            // Set the datapath visualization for THIS step
            this.activeComponents = step.getActiveComponents();
            this.activeBuses = step.getActiveBuses();
            this.busDataValues = step.getBusDataValues();
            
            // Execute the action associated with this step (e.g., the actual register write)
            step.executeAction();

            currentMicroStepIndex++;
        }
        
        // If we just finished the last micro-step, advance the PC and clear the queue
        if (currentMicroStepIndex >= microStepQueue.size()) {
            // PC is advanced by the branch micro-step or by this default
            if (!lastExecutedInstruction.startsWith("B ") && !lastExecutedInstruction.startsWith("CB")) {
                pc++;
            }
            microStepQueue.clear();
            currentMicroStepIndex = 0;
            
            if (pc >= program.size()) {
                isFinished = true;
            }
        }
    }
    /**
     * Generates a sequence of micro-steps for a given instruction.
     * Does NOT execute them.
     */
    private void generateMicroStepsFor(Instruction instruction) {
        microStepQueue.clear();
        currentMicroStepIndex = 0;

        // Based on format, generate the appropriate sequence
        if (instruction instanceof IFormatInstruction) {
            generateIFormatSteps((IFormatInstruction) instruction);
        }
        else if (instruction instanceof RFormatInstruction) {
            generateRFormatSteps((RFormatInstruction) instruction);
        } 
        else if (instruction instanceof BFormatInstruction) {
            generateBFormatSteps((BFormatInstruction) instruction);
        } 
        else if (instruction instanceof DFormatInstruction) {
            generateDFormatSteps((DFormatInstruction) instruction);
        } 
        else if (instruction instanceof IMFormatInstruction) {
            generateIMFormatSteps((IMFormatInstruction) instruction);
        }
        else {
            // Placeholder for unsupported instructions
            microStepQueue.add(new MicroStep("Unsupported Instruction", 
                List.of("CONTROL_UNIT"), List.of(), Map.of(), null));
        }
    }
    // --- GENERATOR FOR I-FORMAT ---
    // I-Format: Instructions like ADDI, SUBI (Immediate arithmetic)
    private void generateIFormatSteps(IFormatInstruction iInst) {
        // --- Instruction Details ---
        String op = iInst.getDefinition().getInstructionName(); // e.g., "ADDI", "SUBI"
        int rd = iInst.getRd_I();
        int rn = iInst.getRn_I();
        long imm = iInst.getImmediate_I();

        // --- Data Values (calculated for simulation) ---
        long rnValue = registerFile.readRegister(rn);
        long result;

        // Determine the result based on the operation
        switch (op.toUpperCase()) {
            case "SUBI":
                result = rnValue - imm;
                break;
            case "ADDI":
            default: // Default to ADDI for simplicity
                result = rnValue + imm;
                break;
        }
        
        String rnName = "R" + rn;
        String rdName = "R" + rd;
        long pcPlus4 = (pc + 1) * 4;

        // --- Micro-Step Generation ---

        // Step 1: Instruction Fetch
        // PC sends its address to Instruction Memory and the PC+4 adder.
        microStepQueue.add(new MicroStep(
            "Instruction Fetch",
            List.of("PC", "INSTRUCTION_MEMORY", "ADD_4", "ADD_1"),
            List.of(
                BusID.PC_TO_INSTRUCTION_MEMORY.name(), BusID.PC_TO_ADD_1.name(), BusID.ADD_4_TO_ADD_1.name()
            ),
            Map.of(
                BusID.PC_TO_INSTRUCTION_MEMORY.name(), String.format("0x%X", pc * 4),
                BusID.PC_TO_ADD_1.name(), String.format("0x%X", pc * 4),
                BusID.ADD_4_TO_ADD_1.name(), "4"
            ),
            null
        ));
        microStepQueue.add(new MicroStep(
            "Instruction Fetch",
            List.of("ADD_1", "MUX_PCSRC"),
            List.of(
                BusID.ADD_1_TO_MUX_PCSRC.name()
            ),
            Map.of(
                BusID.ADD_1_TO_MUX_PCSRC.name(), String.format("0x%X", pcPlus4)
            ),
            null
        ));

        // Step 2: Decode & Register Read
        // The instruction is decoded. The register file reads the value from Rn.
        // The ALUSrc control signal is set to '1' to select the immediate value for the ALU.
        microStepQueue.add(new MicroStep(
            "Decode & Read Register",
            List.of("CONTROL_UNIT", "REGISTERS", "MUX_ALUsrc"),
            List.of(
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(),                 
                BusID.REGISTERS_TO_ALU_READ1.name(), 
                BusID.CONTROL_ALUSRC_TO_MUX_ALUsrc.name()
            ),
            Map.of(
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), iInst.getInstructionHex(),
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), String.format("Imm: %d", imm),
                BusID.REGISTERS_TO_ALU_READ1.name(), String.format("%s: %d", rnName, rnValue),
                BusID.CONTROL_ALUSRC_TO_MUX_ALUsrc.name(), "ALUSrc: 1"
            ),
            null
        ));

        // Step 3: Execute (ALU)
        // The immediate value is sign-extended and sent to the ALU.
        // The ALU performs the operation (ADD/SUB) and outputs the result.
        microStepQueue.add(new MicroStep(
            "Execute (ALU Operation)",
            List.of("SIGN_EXTEND", "MUX_ALUsrc"),
            List.of(
                BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name()
            ),
            Map.of(
                // The sign-extended immediate is selected by the MUX and goes to the ALU
                BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(), String.valueOf(imm)
            ),
            null
        ));
        microStepQueue.add(new MicroStep(
            "Execute (ALU Operation)",
            List.of("ALU", "MUX_ALUsrc"),
            List.of(
                BusID.MUX_ALUsrc_TO_ALU.name(), 
                BusID.ALU_TO_MUX_memtoreg_RESULT.name()
            ),
            Map.of(
                BusID.MUX_ALUsrc_TO_ALU.name() + " (Rn Value)", String.valueOf(rnValue),
                BusID.ALU_TO_MUX_memtoreg_RESULT.name(), String.format("Result: %d", result)
            ),
            null
        ));

        // Step 4: Write-Back
        // The result from the ALU is written back to the destination register (Rd).
        // Control signals RegWrite=1 and MemToReg=0 are set.
        microStepQueue.add(new MicroStep(
            "Write-Back",
            List.of("MUX_memtoreg", "REGISTERS"),
            List.of(
                BusID.CONTROL_MEMTOREG_TO_MUX_memtoreg.name(), 
                BusID.CONTROL_REGWRITE_TO_REGISTERS.name(), 
                BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name(),
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE.name()
            ),
            Map.of(
                BusID.CONTROL_MEMTOREG_TO_MUX_memtoreg.name(), "MemToReg: 0",
                BusID.CONTROL_REGWRITE_TO_REGISTERS.name(), "RegWrite: 1",
                BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name(), String.valueOf(result),
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE.name(), rdName
            ),
            () -> registerFile.writeRegister(rd, result, true)
        ));
    }

    // R-Format: Instructions like ADD, SUB (Register-register arithmetic)
    private void generateRFormatSteps(RFormatInstruction rInst) {
        int rd = rInst.getRd_R();
        int rn = rInst.getRn_R();
        int rm = rInst.getRm_R();
        long rnValue = registerFile.readRegister(rn);
        long rmValue = registerFile.readRegister(rm);
        long result = rnValue + rmValue; // Example for ADD; adjust for other operations

        // Step 1: Instruction Fetch
        microStepQueue.add(new MicroStep(
            "Instruction Fetch",
            List.of("PC", "INSTRUCTION_MEMORY", "ADD_4", "ADD_1"),
            List.of(BusID.PC_TO_INSTRUCTION_MEMORY.name(), BusID.PC_TO_ADD_1.name(), 
                    BusID.ADD_4_TO_ADD_1.name(), BusID.ADD_1_TO_MUX_PCSRC.name()),
            Map.of(
                BusID.PC_TO_INSTRUCTION_MEMORY.name(), String.format("0x%X", pc * 4),
                BusID.ADD_1_TO_MUX_PCSRC.name(), String.format("0x%X", (pc + 1) * 4)
            ),
            null
        ));

        // Step 2: Decode & Register Read
        microStepQueue.add(new MicroStep(
            "Decode & Read Registers",
            List.of("CONTROL_UNIT", "REGISTERS"),
            List.of(BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                    BusID.REGISTERS_TO_ALU_READ1.name(), BusID.REGISTERS_TO_MUX_ALUsrc_READ2.name(),
                    BusID.MUX_ALUsrc_TO_ALU.name()),
            Map.of(
                BusID.REGISTERS_TO_ALU_READ1.name(), String.valueOf(rnValue),
                BusID.REGISTERS_TO_MUX_ALUsrc_READ2.name(), String.valueOf(rmValue)
            ),
            null
        ));

        // Step 3: Execute (ALU)
        microStepQueue.add(new MicroStep(
            "Execute (ALU Operation)",
            List.of("ALU"),
            List.of(BusID.REGISTERS_TO_ALU_READ1.name(), BusID.REGISTERS_TO_MUX_ALUsrc_READ2.name(), 
                    BusID.ALU_TO_MUX_memtoreg_RESULT.name()),
            Map.of(BusID.ALU_TO_MUX_memtoreg_RESULT.name(), String.valueOf(result)),
            null
        ));

        // Step 4: Write-Back
        microStepQueue.add(new MicroStep(
            "Write-Back",
            List.of("MUX_memtoreg", "REGISTERS"),
            List.of(BusID.CONTROL_MEMTOREG_TO_MUX_memtoreg.name(), 
                    BusID.CONTROL_REGWRITE_TO_REGISTERS.name(), 
                    BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name()),
            Map.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name(), String.valueOf(result)),
            () -> registerFile.writeRegister(rd, result, true)
        ));
    }

    // B-Format: Instructions like B (Unconditional branch)
    private void generateBFormatSteps(BFormatInstruction bInst) {
        long imm = bInst.getBranchAddress(pc);
        long branchTarget = pc * 4 + imm; // Branch target address

        // Step 1: Instruction Fetch
        microStepQueue.add(new MicroStep(
            "Instruction Fetch",
            List.of("PC", "INSTRUCTION_MEMORY", "ADD_4", "ADD_1"),
            List.of(BusID.PC_TO_INSTRUCTION_MEMORY.name(), BusID.PC_TO_ADD_1.name(), 
                    BusID.ADD_4_TO_ADD_1.name(), BusID.ADD_1_TO_MUX_PCSRC.name()),
            Map.of(
                BusID.PC_TO_INSTRUCTION_MEMORY.name(), String.format("0x%X", pc * 4),
                BusID.ADD_1_TO_MUX_PCSRC.name(), String.format("0x%X", (pc + 1) * 4)
            ),
            null
        ));

        // Step 2: Decode & Calculate Branch Target
        microStepQueue.add(new MicroStep(
            "Decode & Calculate Branch Target",
            List.of("CONTROL_UNIT", "SIGN_EXTEND", "ADD_BRANCH"),
            List.of(BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                    BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), 
                    BusID.SIGN_EXTEND_TO_SHIFT_LEFT_2.name(), BusID.PC_TO_ADD_2.name(), 
                    BusID.SHIFT_LEFT_2_TO_ADD.name(),
                    BusID.ADD_2_TO_MUX_PCSRC.name()),
            Map.of(
                BusID.SIGN_EXTEND_TO_SHIFT_LEFT_2.name(), String.valueOf(imm),
                BusID.ADD_2_TO_MUX_PCSRC.name(), String.format("0x%X", branchTarget)
            ),
            null
        ));

        // Step 3: Branch (Update PC)
        microStepQueue.add(new MicroStep(
            "Branch (Update PC)",
            List.of("MUX_PCSRC"),
            List.of(BusID.CONTROL_UNCOND_TO_OR_GATE.name(), BusID.ADD_2_TO_MUX_PCSRC.name(),
                    BusID. OR_GATE_TO_MUX_PCSRC.name(),
                    BusID.MUX_PCSRC_TO_PC.name()),
            Map.of(BusID.MUX_PCSRC_TO_PC.name(), String.format("0x%X", branchTarget)),
            () -> pc = (int)branchTarget / 4
        ));
    }
    

    // D-Format: Instructions like LDUR, STUR (Load/Store)
    private void generateDFormatSteps(DFormatInstruction dInst) {
        int rt = dInst.getRt_D();
        int rn = dInst.getRn_D();
        long imm = dInst.getAddress_D();
        long rnValue = registerFile.readRegister(rn);
        long address = rnValue + imm;
        long data = dInst.isLoad() ? memory.read(address, 8) : registerFile.readRegister(rt);

        // Step 1: Instruction Fetch
        microStepQueue.add(new MicroStep(
            "Instruction Fetch",
            List.of("PC", "INSTRUCTION_MEMORY", "ADD_4", "ADD_1"),
            List.of(BusID.PC_TO_INSTRUCTION_MEMORY.name(), BusID.PC_TO_ADD_1.name(), 
                    BusID.ADD_4_TO_ADD_1.name(), BusID.ADD_1_TO_MUX_PCSRC.name()),
            Map.of(
                BusID.PC_TO_INSTRUCTION_MEMORY.name(), String.format("0x%X", pc * 4),
                BusID.ADD_1_TO_MUX_PCSRC.name(), String.format("0x%X", (pc + 1) * 4)
            ),
            null
        ));

        // Step 2: Decode & Register Read
        microStepQueue.add(new MicroStep(
            "Decode & Read Register",
            List.of("CONTROL_UNIT", "REGISTERS"),
            List.of(BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                    BusID.REGISTERS_TO_ALU_READ1.name()),
            Map.of(BusID.REGISTERS_TO_ALU_READ1.name(), String.valueOf(rnValue)),
            null
        ));

        // Step 3: Address Calculation
        microStepQueue.add(new MicroStep(
            "Address Calculation",
            List.of("ALU", "SIGN_EXTEND", "MUX_ALUsrc"),
            List.of(BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), 
                    BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(), BusID.ALU_TO_DATA_MEMORY_ADDRESS.name()),
            Map.of(
                BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(), String.valueOf(imm),
                BusID.ALU_TO_DATA_MEMORY_ADDRESS.name(), String.valueOf(address)
            ),
            null
        ));

        // Step 4: Memory Access
        if (dInst.isLoad()) {
            microStepQueue.add(new MicroStep(
                "Memory Read",
                List.of("DATA_MEMORY", "MUX_memtoreg"),
                List.of(BusID.ALU_TO_DATA_MEMORY_ADDRESS.name(), 
                        BusID.DATA_MEMORY_TO_MUX_memtoreg_READ.name(), 
                        BusID.CONTROL_MEMTOREG_TO_MUX_memtoreg.name()),
                Map.of(BusID.DATA_MEMORY_TO_MUX_memtoreg_READ.name(), String.valueOf(data)),
                null
            ));

            // Step 5: Write-Back (for LDUR)
            microStepQueue.add(new MicroStep(
                "Write-Back",
                List.of("REGISTERS"),
                List.of(BusID.CONTROL_REGWRITE_TO_REGISTERS.name(), 
                        BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name()),
                Map.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name(), String.valueOf(data)),
                () -> registerFile.writeRegister(rt, data, true)
            ));
        } else {
            microStepQueue.add(new MicroStep(
                "Memory Write",
                List.of("DATA_MEMORY", "REGISTERS"),
                List.of(BusID.ALU_TO_DATA_MEMORY_ADDRESS.name(), 
                        BusID.REGISTERS_TO_DATA_MEMORY_WRITE_DATA.name(), 
                        BusID.CONTROL_MEMWRITE_TO_DATA_MEMORY.name()),
                Map.of(BusID.REGISTERS_TO_DATA_MEMORY_WRITE_DATA.name(), String.valueOf(data)),
                () -> memory.write(address, data, 8)
            ));
        }
    }

    // IM-Format: Instructions like MOVZ, MOVK (Move immediate)
    private void generateIMFormatSteps(IMFormatInstruction imInst) {
        int rd = imInst.getRd_IM();
        long imm = imInst.getImmediate_IM();
        long result = imm; // For MOVZ; MOVK requires masking existing register value

        // Step 1: Instruction Fetch
        microStepQueue.add(new MicroStep(
            "Instruction Fetch",
            List.of("PC", "INSTRUCTION_MEMORY", "ADD_4", "ADD_1"),
            List.of(BusID.PC_TO_INSTRUCTION_MEMORY.name(), BusID.PC_TO_ADD_1.name(), 
                    BusID.ADD_4_TO_ADD_1.name(), BusID.ADD_1_TO_MUX_PCSRC.name()),
            Map.of(
                BusID.PC_TO_INSTRUCTION_MEMORY.name(), String.format("0x%X", pc * 4),
                BusID.ADD_1_TO_MUX_PCSRC.name(), String.format("0x%X", (pc + 1) * 4)
            ),
            null
        ));

        // Step 2: Decode
        microStepQueue.add(new MicroStep(
            "Decode",
            List.of("CONTROL_UNIT", "SIGN_EXTEND"),
            List.of(BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                    BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name()),
            Map.of(BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(), String.valueOf(imm)),
            null
        ));

        // Step 3: Write-Back
        microStepQueue.add(new MicroStep(
            "Write-Back",
            List.of("MUX_memtoreg", "REGISTERS"),
            List.of(BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(), 
                    BusID.MUX_ALUsrc_TO_ALU.name(), 
                    BusID.ALU_TO_MUX_memtoreg_RESULT.name(),
                    BusID.CONTROL_REGWRITE_TO_REGISTERS.name(), 
                    BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name()),
            Map.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name(), String.valueOf(result)),
            () -> registerFile.writeRegister(rd, result, true)
        ));
    }

    // --- Reset and Getters ---
    public void reset() {
        program.clear();
        pc = 0;
        registerFile.reset();
        memory.reset();
        isFinished = true;
        microStepQueue.clear();
        currentMicroStepIndex = 0;
        clearDatapathActivity();
        lastExecutedInstruction = "None";
    }

    private void clearDatapathActivity() {
        activeComponents.clear();
        activeBuses.clear();
        busDataValues.clear();
    }

    // --- Public Getters for GUI ---

    public int getPc() { return pc; }
    public long getRegisterValue(int regNum) { return registerFile.readRegister(regNum); }
    public Map<Long, Long> getMemoryState() { return memory.getAllData();}
    public Memory getMemory() { return memory; }
    public boolean isFinished() { return isFinished; }
    public List<String> getActiveComponents() { return new ArrayList<>(activeComponents); }
    public List<String> getActiveBuses() { return new ArrayList<>(activeBuses); }
    public Map<String, String> getBusDataValues() { return new HashMap<>(busDataValues); }
    public String getCurrentMicroStepDescription() {
        if (!microStepQueue.isEmpty() && currentMicroStepIndex < microStepQueue.size()) {
            return microStepQueue.get(currentMicroStepIndex).getDescription();
        }
        return "Ready for next instruction.";
    }
    public String getLastExecutedInstruction() { return lastExecutedInstruction; }
    public int getInstructionCount() { return program.size(); }
    
    public void setPc(int pc) {
        this.pc = pc;
    }

    public boolean isZeroFlag() {
        return zeroFlag;
    }

    public boolean isNegativeFlag() {
        return negativeFlag;
    }   
    public boolean isOverflowFlag() {
        return overflowFlag;
    }
    
    public boolean isCarryFlag() {
        return carryFlag;
    }

    public List<Instruction> getProgram() {
        return program;
    }

    public InstructionFactory getFactory() {
        return factory;
    }

    public RegisterFileController getRegisterFile() {
        return registerFile;
    }

    public ArithmeticLogicUnit getAlu() {
        return alu;
    }

    public ControlUnit getControlUnit() {
        return controlUnit;
    }
}
