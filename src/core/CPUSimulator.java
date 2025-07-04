package core;
import util.*;
import datapath.*;
import instruction.*; 
import memory.Memory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private boolean branchTaken;

    // --- State for GUI Visualization ---
    private List<String> activeComponents;
    private List<String> activeBuses;
    private Map<String, String> busDataValues;
    private String lastExecutedInstruction = "None";
    private boolean isFinished = false;
    
    // --- Execution History System ---
    private ExecutionHistory executionHistory;
    private boolean isRestoringFromHistory = false;

    public CPUSimulator(InstructionConfigLoader configLoader) {
        this.factory = new InstructionFactory(configLoader);
        this.registerFile = new RegisterFileController(new RegisterStorage());
        this.alu = new ArithmeticLogicUnit();
        this.controlUnit = new ControlUnit(configLoader);
        this.memory = new Memory();
        this.program = new ArrayList<>();
        this.microStepQueue = new ArrayList<>();
        
        this.activeComponents = new ArrayList<>();
        this.activeBuses = new ArrayList<>();
        this.busDataValues = new HashMap<>();
        
        // Initialize execution history system
        this.executionHistory = new ExecutionHistory();
        this.isRestoringFromHistory = false;
        
        reset();
    }

    public void loadProgram(String[] assemblyLines) {
        program.clear();
        for (String line : assemblyLines) {
            Instruction instruction = factory.createFromAssembly(line);
            if (instruction != null) {
                program.add(instruction);
            } else {
                System.err.println("Failed to load instruction: " + line);
            }
        }
        System.out.println("Program loaded with " + program.size() + " instruction(s).");
        reset(); // Reset state after loading
        
        // Record initial state to history
        recordCurrentStateToHistory("Program loaded - Initial state");
    }

    /**
     * Executes the entire program until completion.
     */
    public void executeProgram() {
        System.out.println("Program starting.");
        while (!isFinished) {
            step();
        }
        System.out.println("Program finished.");
        printState();
    }

    /**
     * Cập nhật các cờ trạng thái (N, Z, C, V) dựa trên kết quả ALU.
     * Cập nhật cả trong RegisterFile và các biến cục bộ để GUI truy cập.
     */
    private void updateFlags(ArithmeticLogicUnit.ALUResult result) {
        this.negativeFlag = result.n;
        this.zeroFlag = result.z;
        this.carryFlag = result.c;
        this.overflowFlag = result.v;

        // Note: RegisterFileController doesn't have setFlag methods
        // Flags are stored in the CPU simulator instance variables above
    }
    private void generateInstructionFetchSteps() {
        microStepQueue.add(new MicroStep(
            "STep 1: Instruction Fetch",
            List.of("PC", "INSTRUCTION_MEMORY", "ADD_4", "ADD_1"),
            List.of(BusID.PC_TO_INSTRUCTION_MEMORY.name(), BusID.PC_TO_ADD_1.name(),
                    BusID.ADD_4_TO_ADD_1.name()),
            Map.of(
                BusID.PC_TO_INSTRUCTION_MEMORY.name(), String.format("0x%X", pc * 4),
                BusID.PC_TO_ADD_1.name(), String.format("0x%X", pc),
                BusID.ADD_4_TO_ADD_1.name(), "4"
            ),
            null
        ));
        microStepQueue.add(new MicroStep(
            "Step 1: Instruction Fetch",
            List.of("ADD_1", "MUX_PCSRC"),
            List.of(BusID.ADD_1_TO_MUX_PCSRC.name()),
            Map.of(BusID.ADD_1_TO_MUX_PCSRC.name(), String.format("0x%X", (pc + 1) * 4)),
            null
        ));
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

    public void step() {
        if (isFinished) return;

        // If the queue is empty, it's time to fetch and decode the next instruction.
        if (microStepQueue.isEmpty()) {
            if (pc >= program.size()) {
                isFinished = true;
                lastExecutedInstruction = "Execution Complete";
                clearDatapathActivity();
                
                // Record final state to history
                if (!isRestoringFromHistory) {
                    recordCurrentStateToHistory("Program execution completed");
                }
                return;
            }
            Instruction instruction = program.get(pc);
            lastExecutedInstruction = instruction.disassemble();
            this.branchTaken = false; // Reset branch flag for the new instruction
            generateMicroStepsFor(instruction);
            currentMicroStepIndex = 0;
        }
        
        // Execute the current micro-step from the queue.
        if (currentMicroStepIndex < microStepQueue.size()) {
            MicroStep currentStep = microStepQueue.get(currentMicroStepIndex);
            
            // Set visualization state for the GUI
            this.activeComponents = currentStep.getActiveComponents();
            this.activeBuses = currentStep.getActiveBuses();
            this.busDataValues = currentStep.getBusDataValues();
            
            // Record state to history before executing the micro-step
            if (!isRestoringFromHistory) {
                String stepDescription = String.format("Micro-step %d/%d: %s - %s", 
                    currentMicroStepIndex + 1, microStepQueue.size(),
                    lastExecutedInstruction, currentStep.getDescription());
                recordCurrentStateToHistory(stepDescription);
            }
            
            // *** CRITICAL: Execute the action associated with this micro-step ***
            // This is where the actual state of the CPU (registers, memory, PC) changes.
            if (currentStep.getAction() != null) {
                currentStep.getAction().run();
            }

            currentMicroStepIndex++;
        }
        
        // If we've finished all micro-steps for the current instruction...
        if (currentMicroStepIndex >= microStepQueue.size()) {
            microStepQueue.clear();
            currentMicroStepIndex = 0;
            
            // Update PC for the *next* instruction.
            // If a branch was taken, the PC was already updated in a micro-step.
            // Otherwise, increment to the next sequential instruction.
            if (!branchTaken) {
                pc++;
            }

            printState(); // Print state after a full instruction is complete.
            
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
    // I-Format: Immediate arithmetic
    private void generateIFormatSteps(IFormatInstruction iInst) {
        InstructionDefinition definition = iInst.getDefinition();
        ControlSignals signals = controlUnit.generateControlSignals(iInst);
        String mnemonic = definition.getMnemonic();

        int rd = iInst.getRd_I();
        int rn = iInst.getRn_I();
        long immediate = iInst.getImmediate_I(); // Giá trị tức thời đã được mở rộng dấu

        long rnValue = registerFile.readRegister(rn);

        ArithmeticLogicUnit.ALUOperation op = mnemonic.equals("ADDI") ?
            ArithmeticLogicUnit.ALUOperation.ADD : ArithmeticLogicUnit.ALUOperation.SUB;

        final ArithmeticLogicUnit.ALUResult aluResult = alu.execute(rnValue, immediate, op);
        final long result = aluResult.result;

        // --- Micro-Step Generation ---

        // Step 1: Instruction Fetch
        generateInstructionFetchSteps();

        // Step 2: Decode & Register Read
        // The instruction is decoded. The register file reads the value from Rn.
        // The ALUSrc control signal is set to '1' to select the immediate value for the ALU.
        microStepQueue.add(new MicroStep(
            "Step 2: Decode & Register Read",
            new ArrayList<>(List.of("INSTRUCTION_MEMORY", "CONTROL_UNIT", "REGISTERS", "MUX_ALUsrc", "SIGN_EXTEND", "ALU")),
            new ArrayList<>(List.of(
                BusID.INSTRUCTION_MEMORY_.name(),
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(),                 
                BusID.REGISTERS_TO_ALU_READ1.name(), 
                BusID.CONTROL_ALUSRC_TO_MUX_ALUsrc.name()
            )),
            new HashMap<>(Map.of(
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), iInst.getInstructionHex(),
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), String.format("Imm: %d", immediate),
                BusID.REGISTERS_TO_ALU_READ1.name(), String.format("%s: %d", rn, rnValue),
                BusID.CONTROL_ALUSRC_TO_MUX_ALUsrc.name(), "ALUSrc: 1"
            )),
            null
        ));

        // Step 3: Execute (ALU)
        // The immediate value is sign-extended and sent to the ALU.
        // The ALU performs the operation (ADD/SUB) and outputs the result.
        microStepQueue.add(new MicroStep(
            "Step 3: Execute (ALU)",
            new ArrayList<>(List.of("SIGN_EXTEND", "MUX_ALUsrc")),
            new ArrayList<>(List.of(
                BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name()
            )),
            new HashMap<>(Map.of(
                BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(), String.valueOf(immediate)
            )),
            null
        ));
        microStepQueue.add(new MicroStep(
            "Step 3: Execute (ALU)",
            new ArrayList<>(List.of("ALU", "MUX_ALUsrc", "MUX_memtoreg")),
            new ArrayList<>(List.of(
                BusID.MUX_ALUsrc_TO_ALU.name(), 
                BusID.ALU_TO_MUX_memtoreg_RESULT.name()
            )),
            new HashMap<>(Map.of(
                BusID.MUX_ALUsrc_TO_ALU.name() + " (Rn Value)", String.valueOf(rnValue),
                BusID.ALU_TO_MUX_memtoreg_RESULT.name(), String.format("%d", result)
            )),
            null
        ));

        // Step 4: Write-Back
        // The result from the ALU is written back to the destination register (Rd).
        // Control signals RegWrite=1 and MemToReg=0 are set.
        if (signals.isRegWrite()) {
            microStepQueue.add(new MicroStep(
                "Step 4: Write Back",
                new ArrayList<>(List.of("MUX_MEMTOREG", "REGISTERS")),
                new ArrayList<>(List.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name())),
                new HashMap<>(Map.of(
                    BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name(), String.format("%d", result)
                )),
                () -> {
                    registerFile.writeRegister(rd, result, true);
                    if (signals.isFlagWrite()) {
                        updateFlags(aluResult);
                    }
                }
            ));
        } else {
            microStepQueue.add(new MicroStep(
                "Step 4: Can not write back",
                new ArrayList<>(List.of()), new ArrayList<>(List.of()),
                new HashMap<>(Map.of("INFO", String.format("%s.", mnemonic))),
                () -> {
                    if (signals.isFlagWrite()) {
                        updateFlags(aluResult);
                    }
                }
            ));
        }
    }

    // R-Format: Register-register arithmetic
    private void generateRFormatSteps(RFormatInstruction rInst) {
        InstructionDefinition definition = rInst.getDefinition();
        ControlSignals signals = definition.getControlSignals();
        String mnemonic = definition.getMnemonic();

        int rd = rInst.getRd_R();
        int rn = rInst.getRn_R();
        int rm = rInst.getRm_R();
        int shamt = rInst.getShamt_R();

        long rnValue = registerFile.readRegister(rn);
        long rmValue = registerFile.readRegister(rm);

        // Determine ALU operation from mnemonic
        ArithmeticLogicUnit.ALUOperation op;
        long operandB = rmValue; // Default second operand

        switch (mnemonic) {
            case "ADD":  op = ArithmeticLogicUnit.ALUOperation.ADD; break;
            case "SUB":  op = ArithmeticLogicUnit.ALUOperation.SUB; break;
            case "AND":  op = ArithmeticLogicUnit.ALUOperation.AND; break;
            case "ORR":  op = ArithmeticLogicUnit.ALUOperation.ORR; break;
            case "EOR":  op = ArithmeticLogicUnit.ALUOperation.EOR; break;
            case "MUL":  op = ArithmeticLogicUnit.ALUOperation.MUL; break;
            case "SDIV": op = ArithmeticLogicUnit.ALUOperation.SDIV; break;
            case "UDIV": op = ArithmeticLogicUnit.ALUOperation.UDIV; break;
            case "CMP":  op = ArithmeticLogicUnit.ALUOperation.SUB; break;
            case "SMULH": op = ArithmeticLogicUnit.ALUOperation.SMULH; break;
            case "UMULH": op = ArithmeticLogicUnit.ALUOperation.UMULH; break;
            case "LSL":
                op = ArithmeticLogicUnit.ALUOperation.LSL;
                operandB = shamt; // Use shift amount
                break;
            case "LSR":
                op = ArithmeticLogicUnit.ALUOperation.LSR;
                operandB = shamt;
                break;
            case "ASR":
                op = ArithmeticLogicUnit.ALUOperation.ASR;
                operandB = shamt;
                break;
            default:
                op = ArithmeticLogicUnit.ALUOperation.ADD; // Default fallback
        }

        final ArithmeticLogicUnit.ALUResult aluResult = alu.execute(rnValue, operandB, op);
        final long result = aluResult.result;

        // Step 1: Instruction Fetch
        generateInstructionFetchSteps();

        // Step 2: Decode & Register Read
        microStepQueue.add(new MicroStep(
            "Step 2: Decode & Register Read",
            new ArrayList<>(List.of("CONTROL_UNIT", "REGISTERS")),
            new ArrayList<>(List.of(BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                    BusID.REGISTERS_TO_ALU_READ1.name(), BusID.REGISTERS_TO_MUX_ALUsrc_READ2.name(),
                    BusID.MUX_ALUsrc_TO_ALU.name())),
            new HashMap<>(Map.of(
                BusID.REGISTERS_TO_ALU_READ1.name(), String.valueOf(rnValue),
                BusID.REGISTERS_TO_MUX_ALUsrc_READ2.name(), String.valueOf(rmValue)
            )),
            null
        ));

        // Step 3: Execute (ALU)
        microStepQueue.add(new MicroStep(
            "Step 3: Execute (ALU)",
            new ArrayList<>(List.of("ALU")),
            new ArrayList<>(List.of(BusID.REGISTERS_TO_ALU_READ1.name(), BusID.REGISTERS_TO_MUX_ALUsrc_READ2.name(), 
                    BusID.ALU_TO_MUX_memtoreg_RESULT.name())),
            new HashMap<>(Map.of(
                BusID.ALU_TO_MUX_memtoreg_RESULT.name(), String.valueOf(result))),
            null
        ));

        // Step 4: Write-Back
        if (signals.isRegWrite()) {
            microStepQueue.add(new MicroStep(
                "Step 4: Write-Back",
                new ArrayList<>(List.of("MUX_memtoreg", "REGISTERS")),
                new ArrayList<>(List.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name())),
                new HashMap<>(Map.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name(), String.valueOf(result))),
                () -> {
                    registerFile.writeRegister(rd, result, true);
                    if (signals.isFlagWrite()) {
                        updateFlags(aluResult);
                    }
                }
            ));
        } else {
            // If RegWrite is false, we still need to update the flags if FlagW is set
            if (signals.isFlagWrite()) {
                updateFlags(aluResult);
            }
        }
    }

    // B-Format: Instructions like B (Unconditional branch)
    private void generateBFormatSteps(BFormatInstruction bInst) {
        long offset = bInst.getAddress_B();
        final int targetPc = this.pc + (int) offset;

        // Step 1: Instruction Fetch
        generateInstructionFetchSteps();

        // Step 2: Decode & Calculate Branch Target
        microStepQueue.add(new MicroStep(
            "Step 2: Decode & Calculate Branch Target",
            new ArrayList<>(List.of("CONTROL_UNIT", "SIGN_EXTEND", "ADD_BRANCH", "SHIFT_LEFT_2")),
            new ArrayList<>(List.of(BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                    BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), 
                    BusID.SIGN_EXTEND_TO_SHIFT_LEFT_2.name(), BusID.PC_TO_ADD_2.name(), 
                    BusID.SHIFT_LEFT_2_TO_ADD.name(),
                    BusID.ADD_2_TO_MUX_PCSRC.name())),
            new HashMap<>(Map.of(
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), bInst.getInstructionHex(),
                BusID.PC_TO_ADD_2.name(), String.format("0x%X", this.pc),
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), String.format("Imm: %d", offset),
                BusID.SIGN_EXTEND_TO_SHIFT_LEFT_2.name(), String.valueOf(offset),
                BusID.ADD_2_TO_MUX_PCSRC.name(), String.format("0x%X", targetPc)
            )),
            null
        ));

        // Step 3: Branch (Update PC)
        microStepQueue.add(new MicroStep(
            "Step 3: Branch (Update PC)",
            new ArrayList<>(List.of("MUX_PCSRC", "PC")),
            new ArrayList<>(List.of(BusID.CONTROL_UNCOND_TO_OR_GATE.name(), BusID.ADD_2_TO_MUX_PCSRC.name(),
                    BusID. OR_GATE_TO_MUX_PCSRC.name(),
                    BusID.MUX_PCSRC_TO_PC.name())),
            new HashMap<>(Map.of(
                BusID.CONTROL_UNCOND_TO_OR_GATE.name(), "1", // Unconditional branch
                BusID.MUX_PCSRC_TO_PC.name(), String.format("0x%X", targetPc * 4))),
            () -> this.pc = targetPc
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
        generateInstructionFetchSteps();

        // Step 2: Decode & Register Read
        microStepQueue.add(new MicroStep(
            "Step 2: Decode & Read Register",
            new ArrayList<>(List.of("CONTROL_UNIT", "REGISTERS")),
            new ArrayList<>(List.of(BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                    BusID.REGISTERS_TO_ALU_READ1.name())),
            new HashMap<>(Map.of(BusID.REGISTERS_TO_ALU_READ1.name(), String.valueOf(rnValue))),
            null
        ));

        // Step 3: Address Calculation
        microStepQueue.add(new MicroStep(
            "Step 3: Address Calculation",
            new ArrayList<>(List.of("ALU", "SIGN_EXTEND", "MUX_ALUsrc")),
            new ArrayList<>(List.of(BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), 
                    BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(), BusID.ALU_TO_DATA_MEMORY_ADDRESS.name())),
            new HashMap<>(Map.of(
                BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(), String.valueOf(imm),
                BusID.ALU_TO_DATA_MEMORY_ADDRESS.name(), String.valueOf(address)
            )),
            null
        ));

        // Step 4: Memory Access
        if (dInst.isLoad()) {
            microStepQueue.add(new MicroStep(
                "Step 4: Memory Read",
                new ArrayList<>(List.of("DATA_MEMORY", "MUX_memtoreg")),
                new ArrayList<>(List.of(BusID.ALU_TO_DATA_MEMORY_ADDRESS.name(), 
                        BusID.DATA_MEMORY_TO_MUX_memtoreg_READ.name(), 
                        BusID.CONTROL_MEMTOREG_TO_MUX_memtoreg.name())),
                new HashMap<>(Map.of(BusID.DATA_MEMORY_TO_MUX_memtoreg_READ.name(), String.valueOf(data))),
                null
            ));

            // Step 5: Write-Back (for LDUR)
            microStepQueue.add(new MicroStep(
                "Step 5: Write-Back",
                new ArrayList<>(List.of("REGISTERS")),
                new ArrayList<>(List.of(BusID.CONTROL_REGWRITE_TO_REGISTERS.name(), 
                        BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name())),
                new HashMap<>(Map.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name(), String.valueOf(data))),
                () -> registerFile.writeRegister(rt, data, true)
            ));
        } else {
            microStepQueue.add(new MicroStep(
                "Step 5: Memory Write",
                new ArrayList<>(List.of("DATA_MEMORY", "REGISTERS")),
                new ArrayList<>(List.of(BusID.ALU_TO_DATA_MEMORY_ADDRESS.name(), 
                        BusID.REGISTERS_TO_DATA_MEMORY_WRITE_DATA.name(), 
                        BusID.CONTROL_MEMWRITE_TO_DATA_MEMORY.name())),
                new HashMap<>(Map.of(BusID.REGISTERS_TO_DATA_MEMORY_WRITE_DATA.name(), String.valueOf(data))),
                () -> memory.write(address, data, 8)
            ));
        }
    }

    // IM-Format: Instructions like MOVZ, MOVK (Move immediate)
    private void generateIMFormatSteps(IMFormatInstruction imInst) {
        int rd = imInst.getRd_IM();
        int hw = imInst.getShift_IM();
        long imm16 = imInst.getImmediate_IM();
        int shiftAmount = hw * 16;
        final long result = imm16 << shiftAmount;

        // Step 1: Instruction Fetch
        generateInstructionFetchSteps();

        // Step 2: Decode
        microStepQueue.add(new MicroStep(
            "Decode",
            new ArrayList<>(List.of("CONTROL_UNIT", "SIGN_EXTEND", "MUX_ALUsrc", "INSTRUCTION_MEMORY")),
            new ArrayList<>(List.of(BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                    BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name())),
            new HashMap<>(Map.of(BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(), String.valueOf(imm16))),
            null
        ));

        // Step 3: Write-Back
        microStepQueue.add(new MicroStep(
            "Execute",
            new ArrayList<>(List.of("ALU", "MUX_memtoreg")), // Component tham gia có thể là Shifter hoặc ALU
            new ArrayList<>(List.of(BusID.ALU_TO_MUX_memtoreg_RESULT.name())),
            new HashMap<>(Map.of(
                BusID.ALU_TO_MUX_memtoreg_RESULT.name(), String.format("0x%X", result)
            )),
            null
        ));
        // Step 4
        microStepQueue.add(new MicroStep(
            "Write-Back",
            new ArrayList<>(List.of("MUX_memtoreg", "REGISTERS")),
            new ArrayList<>(List.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name())),
            new HashMap<>(Map.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name(), String.valueOf(result))),
            () -> registerFile.writeRegister(rd, result, true)
        ));
    }

    public void reset() {
        pc = 0;
        registerFile.reset();
        memory.reset();
        isFinished = false;
        branchTaken = false;
        microStepQueue.clear();
        currentMicroStepIndex = 0;
        clearDatapathActivity();
        lastExecutedInstruction = "None";
        zeroFlag = false;
        negativeFlag = false;
        overflowFlag = false;
        carryFlag = false;
        
        // Clear execution history when resetting
        if (executionHistory != null) {
            executionHistory.clear();
        }
    }

    private void clearDatapathActivity() {
        activeComponents.clear();
        activeBuses.clear();
        busDataValues.clear();
    }
    
    // --- Execution History Methods ---
    
    /**
     * Records the current execution state to history.
     */
    private void recordCurrentStateToHistory(String stepDescription) {
        if (isRestoringFromHistory) return; // Prevent recursive recording
        
        // Collect current register values
        Map<Integer, Long> registerValues = new HashMap<>();
        for (int i = 0; i < 32; i++) {
            registerValues.put(i, registerFile.readRegister(i));
        }
        
        // Get current memory state (only modified values for efficiency)
        Map<Long, Long> memoryValues = memory.getAllData();
        
        // Create execution state
        ExecutionState state = new ExecutionState(
            pc,
            zeroFlag,
            negativeFlag,
            overflowFlag,
            carryFlag,
            lastExecutedInstruction,
            isFinished,
            currentMicroStepIndex,
            registerValues,
            memoryValues,
            new ArrayList<>(activeComponents),
            new ArrayList<>(activeBuses),
            new HashMap<>(busDataValues),
            stepDescription
        );
        
        executionHistory.addState(state);
    }
    
    /**
     * Steps back to the previous execution state.
     * @return true if step back was successful, false if already at beginning
     */
    public boolean stepBack() {
        ExecutionState previousState = executionHistory.stepBack();
        if (previousState == null) {
            return false;
        }
        
        restoreFromState(previousState);
        return true;
    }
    
    /**
     * Steps forward to the next execution state (after stepping back).
     * @return true if step forward was successful, false if already at end
     */
    public boolean stepForward() {
        ExecutionState nextState = executionHistory.stepForward();
        if (nextState == null) {
            return false;
        }
        
        restoreFromState(nextState);
        return true;
    }
    
    /**
     * Restores the CPU state from an ExecutionState.
     */
    private void restoreFromState(ExecutionState state) {
        isRestoringFromHistory = true;
        
        try {
            // Restore CPU state
            this.pc = state.getProgramCounter();
            this.zeroFlag = state.isZeroFlag();
            this.negativeFlag = state.isNegativeFlag();
            this.overflowFlag = state.isOverflowFlag();
            this.carryFlag = state.isCarryFlag();
            this.lastExecutedInstruction = state.getLastExecutedInstruction();
            this.isFinished = state.isFinished();
            this.currentMicroStepIndex = state.getCurrentMicroStepIndex();
            
            // Restore register values
            Map<Integer, Long> registers = state.getRegisterValues();
            registerFile.reset(); // Clear all registers first
            for (Map.Entry<Integer, Long> entry : registers.entrySet()) {
                registerFile.writeRegister(entry.getKey(), entry.getValue(), false); // Don't log register writes during restore
            }
            
            // Restore memory values
            memory.reset(); // Clear memory first
            Map<Long, Long> memoryState = state.getModifiedMemoryValues();
            for (Map.Entry<Long, Long> entry : memoryState.entrySet()) {
                memory.write(entry.getKey(), entry.getValue(), 8);
            }
            
            // Restore visualization state
            this.activeComponents = new ArrayList<>(state.getActiveComponents());
            this.activeBuses = new ArrayList<>(state.getActiveBuses());
            this.busDataValues = new HashMap<>(state.getBusDataValues());
            
            // Clear micro-step queue since we're restoring to a specific state
            this.microStepQueue.clear();
            
            System.out.println("State restored: " + state.getStepDescription());
        } finally {
            isRestoringFromHistory = false;
        }
    }
    
    /**
     * Checks if we can step back in execution history.
     */
    public boolean canStepBack() {
        return executionHistory.canStepBack();
    }
    
    /**
     * Checks if we can step forward in execution history.
     */
    public boolean canStepForward() {
        return executionHistory.canStepForward();
    }
    
    /**
     * Gets the current step description from history.
     */
    public String getCurrentHistoryStepDescription() {
        ExecutionState currentState = executionHistory.getCurrentState();
        return currentState != null ? currentState.getStepDescription() : "No history available";
    }
    
    /**
     * Gets execution history statistics.
     */
    public String getExecutionStatistics() {
        return executionHistory.getStatistics();
    }
    
    /**
     * Gets the execution history manager.
     */
    public ExecutionHistory getExecutionHistory() {
        return executionHistory;
    }
    
    /**
     * Clears the execution history.
     */
    public void clearExecutionHistory() {
        executionHistory.clear();
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
    public boolean isZeroFlag() { return zeroFlag; }
    public boolean isNegativeFlag() { return negativeFlag; }
    public boolean isOverflowFlag() { return overflowFlag; }
    public boolean isCarryFlag() { return carryFlag; }
    public List<Instruction> getProgram() { return program; }
    public RegisterFileController getRegisterFile() { return registerFile; }
    
    // --- Setters for GUI and History Restoration ---
    public void setPc(int pc) {
        this.pc = pc;
    }
    
    public void setFlags(boolean zero, boolean negative, boolean overflow, boolean carry) {
        this.zeroFlag = zero;
        this.negativeFlag = negative;
        this.overflowFlag = overflow;
        this.carryFlag = carry;
    }
    
    public void setFinished(boolean finished) {
        this.isFinished = finished;
    }
    
    public void setLastExecutedInstruction(String instruction) {
        this.lastExecutedInstruction = instruction;
    }
    
    public void setCurrentMicroStepIndex(int index) {
        this.currentMicroStepIndex = index;
    }
    
    /**
     * Sets the visualization state for GUI updates.
     */
    public void setVisualizationState(List<String> activeComponents, List<String> activeBuses, 
                                     Map<String, String> busDataValues) {
        this.activeComponents = new ArrayList<>(activeComponents);
        this.activeBuses = new ArrayList<>(activeBuses);
        this.busDataValues = new HashMap<>(busDataValues);
    }

    public int getCurrentMicroStepIndex() {
        return currentMicroStepIndex;
    }
    
    public int getTotalMicroSteps() {
        return microStepQueue.size();
    }
    
    public boolean hasMoreMicroSteps() {
        return currentMicroStepIndex < microStepQueue.size();
    }
}
