package core;

import datapath.*;
import instruction.*;
import memory.Memory;
import util.*;
import exceptions.MemoryAccessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages micro-step execution for the LEGv8 CPU simulator.
 * This class handles the generation and execution of micro-steps for each instruction type.
 */
public class MicroStepManager {
    
    /**
     * Interface for updating CPU flags from ALU results
     */
    @FunctionalInterface
    public interface FlagUpdater {
        void updateFlags(ArithmeticLogicUnit.ALUResult result);
    }
    
    // Dependencies
    private final RegisterFileController registerFile;
    private final ArithmeticLogicUnit alu;
    private final ControlUnit controlUnit;
    private final Memory memory;
    private FlagUpdater flagUpdater;
    
    // Micro-step execution state
    private List<MicroStep> microStepQueue;
    private int currentMicroStepIndex;
    
    // CPU state references (passed from CPUSimulator)
    private int pc;
    private CPUSimulator cpu;
    private boolean zeroFlag;
    private boolean negativeFlag;
    private boolean overflowFlag;
    private boolean carryFlag;
    private boolean takeTheBranch;
    
    public MicroStepManager(CPUSimulator cpu, RegisterFileController registerFile, ArithmeticLogicUnit alu, 
                           ControlUnit controlUnit, Memory memory, FlagUpdater flagUpdater) {
        this.registerFile = registerFile;
        this.alu = alu;
        this.controlUnit = controlUnit;
        this.memory = memory;
        this.flagUpdater = flagUpdater;
        this.microStepQueue = new ArrayList<>();
        this.currentMicroStepIndex = 0;
        this.cpu = cpu;
    }
    
    /**
     * Updates the CPU state references used by micro-steps
     */
    public void updateCPUState(int pc, boolean zeroFlag, boolean negativeFlag, 
                               boolean overflowFlag, boolean carryFlag, boolean branchTaken) {
        this.pc = pc;
        this.zeroFlag = zeroFlag;
        this.negativeFlag = negativeFlag;
        this.overflowFlag = overflowFlag;
        this.carryFlag = carryFlag;
    }
    
    /**
     * Generates micro-steps for the given instruction
     */
    public void generateMicroStepsFor(Instruction instruction, boolean zeroFlag) {
        microStepQueue.clear();
        currentMicroStepIndex = 0;

        // Based on format, generate the appropriate sequence
        if (instruction instanceof IFormatInstruction) {
            generateIFormatSteps((IFormatInstruction) instruction, zeroFlag);
        }
        else if (instruction instanceof RFormatInstruction) {
            generateRFormatSteps((RFormatInstruction) instruction, zeroFlag);
        } 
        else if (instruction instanceof BFormatInstruction) {
            generateBFormatSteps((BFormatInstruction) instruction, zeroFlag);
        } 
        else if (instruction instanceof DFormatInstruction) {
            generateDFormatSteps((DFormatInstruction) instruction, zeroFlag);
        } 
        else if (instruction instanceof IMFormatInstruction) {
            generateIMFormatSteps((IMFormatInstruction) instruction, zeroFlag);
        }
        else {
            // Unsupported instruction type, add a placeholder step
            microStepQueue.add(new MicroStep(
                "Unsupported Instruction",
                PipelineStage.NONE,
                List.of("CONTROL_UNIT"),
                List.of(),
                Map.of(),
                null
            ));
        }
    }
    
    /**
     * Gets the current micro-step if available
     */
    public MicroStep getCurrentMicroStep() {
        if (currentMicroStepIndex < microStepQueue.size()) {
            return microStepQueue.get(currentMicroStepIndex);
        }
        return null;
    }
    public List<MicroStep> getMicroStepQueue() {
        return microStepQueue;
    }
    /**
     * Advances to the next micro-step
     */
    public void advanceToNextMicroStep() {
        currentMicroStepIndex++;
    }
    
    /**
     * Checks if there are more micro-steps to execute
     */
    public boolean hasMoreMicroSteps() {
        return currentMicroStepIndex < microStepQueue.size();
    }
    
    /**
     * Resets the micro-step execution
     */
    public void reset() {
        microStepQueue.clear();
        currentMicroStepIndex = 0;
    }
    
    /**
     * Gets the current micro-step description
     */
    public String getCurrentMicroStepDescription() {
        if (!microStepQueue.isEmpty() && currentMicroStepIndex < microStepQueue.size()) {
            return microStepQueue.get(currentMicroStepIndex).getDescription();
        }
        return "No active micro-step";
    }

    // Getters for external access
    public int getCurrentMicroStepIndex() { return currentMicroStepIndex; }
    public int getTotalMicroSteps() { return microStepQueue.size(); }
    // public List<MicroStep> getMicroStepQueue() { return new ArrayList<>(microStepQueue); }
    public boolean isEmpty() { return microStepQueue.isEmpty() || currentMicroStepIndex >= microStepQueue.size(); }
    
    // Setters for state restoration
    public void setCurrentMicroStepIndex(int index) { this.currentMicroStepIndex = index; }
    
    // --- Private methods for generating instruction-specific micro-steps ---
    
    private void generateInstructionFetchSteps() {
        microStepQueue.add(new MicroStep(
            "Step 1: Instruction Fetch",
            PipelineStage.FETCH,
            List.of("PC", "INSTRUCTION_MEMORY", "ADD_4", "ADD_1", "MUX_PCSRC"),
            List.of(BusID.PC_TO_INSTRUCTION_MEMORY.name(), BusID.PC_TO_ADD_1.name(),
                    BusID.ADD_4_TO_ADD_1.name(),
                    BusID.ADD_1_TO_MUX_PCSRC.name()),  
            Map.of(
                BusID.PC_TO_INSTRUCTION_MEMORY.name(), String.format("0x%X", pc * 4),
                BusID.PC_TO_ADD_1.name(), String.format("0x%X", pc),
                BusID.ADD_1_TO_MUX_PCSRC.name(), String.format("0x%X", (pc + 1) * 4),
                BusID.ADD_4_TO_ADD_1.name(), "4"
            ),
            null
        ));
    }
    /**
     * Tính toán giá trị của các bus liên quan đến logic rẽ nhánh cho mục đích hiển thị (highlighting).
     * @param signals Tín hiệu điều khiển tĩnh cho lệnh hiện tại.
     * @param branchConditionMet Kết quả của việc đánh giá điều kiện rẽ nhánh (chỉ có ý nghĩa nếu isFlagBranch là true).
     * @return Một Map chứa tên bus và giá trị (0 hoặc 1) của chúng.
     */
    private Map<String, String> generateBranchLogicBusValues(ControlSignals signals, boolean branchConditionMet) {
        Map<String, String> busValues = new HashMap<>();

        // Handle AND2 gate for zero branch conditions (B.EQ, B.NE)
        boolean zeroBranchSignal = signals.isZeroBranch() && this.zeroFlag;
        busValues.put(BusID.ALU_TO_AND2_GATE.name(), signals.isZeroBranch() ? "1" : "0");
        busValues.put(BusID.AND2_GATE_TO_OR_GATE.name(), zeroBranchSignal ? "1" : "0");

        // Handle AND gate for flag branch conditions (B.LT, B.GT, B.LE, B.GE)
        boolean flagBranchSignal = signals.isFlagBranch() && branchConditionMet;
        busValues.put(BusID.FLAG_TO_AND_GATE.name(), branchConditionMet ? "1" : "0");
        busValues.put(BusID.AND_GATE_TO_OR_GATE.name(), flagBranchSignal ? "1" : "0");
        
        // Final signal to PC source MUX
        // This signal is '1' if:
        // 1. Unconditional branch (UncondBranch)
        // OR
        // 2. Conditional branch AND condition is met (FlagBranch + branchConditionMet)
        // OR 
        // 3. Zero branch AND zero flag is set (ZeroBranch + zeroFlag)
        boolean takeBranch = signals.isUncondBranch() || flagBranchSignal || zeroBranchSignal;
        busValues.put(BusID.OR_GATE_TO_MUX_PCSRC.name(), takeBranch ? "1" : "0");
        
        return busValues;
    }
    
    private void generateControlUnitSteps(ControlSignals signals, InstructionDefinition definition, boolean zeroFlag) {
        Map<String, String> controlBusMap = new HashMap<>();
        controlBusMap.put(BusID.CONTROL_ALUOP_TO_ALU_CONTROL.name(), String.format("%2s", Integer.toBinaryString(signals.getAluOp())).replace(' ', '0'));
        controlBusMap.put(BusID.CONTROL_ALUSRC_TO_MUX_ALUsrc.name(), signals.isAluSrc() ? "1" : "0");
        controlBusMap.put(BusID.CONTROL_MEMREAD_TO_DATA_MEMORY.name(), signals.isMemRead() ? "1" : "0");
        controlBusMap.put(BusID.CONTROL_MEMWRITE_TO_DATA_MEMORY.name(), signals.isMemWrite() ? "1" : "0");
        controlBusMap.put(BusID.CONTROL_REGWRITE_TO_REGISTERS.name(), signals.isRegWrite() ? "1" : "0");
        controlBusMap.put(BusID.CONTROL_MEMTOREG_TO_MUX_memtoreg.name(), signals.isMemToReg() ? "1" : "0");
        controlBusMap.put(BusID.CONTROL_UNCOND_TO_OR_GATE.name(), signals.isUncondBranch() ? "1" : "0");
        controlBusMap.put(BusID.CONTROL_FLAG_BRANCH_TO_AND1_GATE.name(), signals.isFlagBranch() ? "1" : "0");
        controlBusMap.put(BusID.CONTROL_FLAGWRITE_TO_REGISTERS.name(), signals.isFlagWrite() ? "1" : "0");
        controlBusMap.put(BusID.CONTROL_REG2LOC_TO_MUX_reg2loc.name(), signals.isReg2Loc() ? "1" : "0");
        controlBusMap.put(BusID.CONTROL_ZERO_BRANCH_TO_AND2_GATE.name(), signals.isZeroBranch() ? "1" : "0");
        controlBusMap.put(BusID.ALU_CONTROL_TO_ALU.name(), generateAluControlSignal(signals.getAluOp(), definition.getOpcodeId()));
        
        // Generate branch logic bus values and integrate them into the control bus map
        String conditionCode = definition.getMnemonic().contains(".") ? 
            definition.getMnemonic().substring(definition.getMnemonic().indexOf('.') + 1) : "EQ";
        boolean branchConditionMet = cpu.evaluateBranchCondition(conditionCode);
        Map<String, String> branchBusValues = generateBranchLogicBusValues(signals, branchConditionMet);
        controlBusMap.putAll(branchBusValues);

        microStepQueue.add(new MicroStep(
            "Step 2: Control Signal Generation",
            PipelineStage.DECODE,
            new ArrayList<>(List.of("CONTROL_UNIT", "AND_GATE", "AND2_GATE", "OR_GATE")),
            new ArrayList<>(List.of(
                BusID.CONTROL_ALUOP_TO_ALU_CONTROL.name(),
                BusID.CONTROL_ALUSRC_TO_MUX_ALUsrc.name(),
                BusID.CONTROL_MEMREAD_TO_DATA_MEMORY.name(),
                BusID.CONTROL_MEMWRITE_TO_DATA_MEMORY.name(),
                BusID.CONTROL_REGWRITE_TO_REGISTERS.name(),
                BusID.CONTROL_MEMTOREG_TO_MUX_memtoreg.name(),
                BusID.CONTROL_UNCOND_TO_OR_GATE.name(),
                BusID.CONTROL_FLAG_BRANCH_TO_AND1_GATE.name(),
                BusID.CONTROL_FLAGWRITE_TO_REGISTERS.name(),
                BusID.CONTROL_REG2LOC_TO_MUX_reg2loc.name(),
                BusID.CONTROL_ZERO_BRANCH_TO_AND2_GATE.name(),
                BusID.ALU_CONTROL_TO_ALU.name(),
                BusID.AND_GATE_TO_OR_GATE.name(),
                BusID.AND2_GATE_TO_OR_GATE.name(),
                BusID.OR_GATE_TO_MUX_PCSRC.name(),
                BusID.ALU_TO_AND2_GATE.name(),
                BusID.FLAG_TO_AND_GATE.name()
            )),
            controlBusMap,
            () -> {
                try {
                    controlUnit.setControlSignals(signals);
                    System.out.println("Control signals set successfully");
                } catch (Exception e) {
                    System.err.println("Error setting control signals: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        ));
    }
    
    // --- I-Format instruction micro-steps ---
    private void generateIFormatSteps(IFormatInstruction iInst, boolean zeroFlag) {
        InstructionDefinition definition = iInst.getDefinition();
        ControlSignals signals = controlUnit.generateControlSignals(iInst);
        String mnemonic = definition.getMnemonic();

        int rd = iInst.getRd_I();
        int rn = iInst.getRn_I();
        long immediate = iInst.getImmediate_I();

        long rnValue = registerFile.readRegister(rn);

        ArithmeticLogicUnit.ALUOperation op = mnemonic.equals("ADDI") ?
            ArithmeticLogicUnit.ALUOperation.ADD : ArithmeticLogicUnit.ALUOperation.SUB;

        final ArithmeticLogicUnit.ALUResult aluResult = alu.execute(rnValue, immediate, op);
        final long result = aluResult.result;

        // Step 1: Instruction Fetch
        generateInstructionFetchSteps();

        // Step 2: Decode & Register Read
        microStepQueue.add(new MicroStep(
            "Step 2: Decode & Register Read",
            PipelineStage.DECODE,
            new ArrayList<>(List.of("INSTRUCTION_MEMORY", "CONTROL_UNIT", "REGISTERS", "SIGN_EXTEND")),
            new ArrayList<>(List.of(
                BusID.INSTRUCTION_MEMORY_.name(),
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), 
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_READ1.name(),   
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE.name(),
                BusID.INSTRUCTION_MEMORY_TO_ALU_CONTROL.name()             
            )),
            new HashMap<>(Map.of(
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), definition.getOpcodeId(),
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), String.format("%32s", Long.toBinaryString(immediate & 0xFFF)).replace(' ', '0'),
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_READ1.name(), String.format("%5s", Integer.toBinaryString(rn & 0x1F)).replace(' ', '0'),
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE.name(), String.format("%5s", Integer.toBinaryString(rd & 0x1F)).replace(' ', '0'),
                BusID.INSTRUCTION_MEMORY_TO_ALU_CONTROL.name(), String.format("%10s", Integer.toBinaryString(signals.getAluOp())).replace(' ', '0')
            )),
            null
        ));
        
        // Step 2: Control Signal Generation
        generateControlUnitSteps(signals, definition, zeroFlag);

        // Step 3: Execute (ALU)
        microStepQueue.add(new MicroStep(
            "Step 3: Execute (ALU)",
            PipelineStage.EXECUTE,
            new ArrayList<>(List.of("SIGN_EXTEND", "MUX_ALUsrc", "ALU", "REGISTERS")),
            new ArrayList<>(List.of(
                BusID.REGISTERS_TO_ALU_READ1.name(),
                BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name()
            )),
            new HashMap<>(Map.of(
                BusID.REGISTERS_TO_ALU_READ1.name(), String.valueOf(rnValue),
                BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(), String.valueOf(immediate)
            )),
            null
        ));
        microStepQueue.add(new MicroStep(
            "Step 3: Execute (ALU)",
            PipelineStage.EXECUTE,
            new ArrayList<>(List.of("ALU", "MUX_ALUsrc", "MUX_memtoreg")),
            new ArrayList<>(List.of(
                BusID.MUX_ALUsrc_TO_ALU.name(), 
                BusID.ALU_TO_MUX_memtoreg_RESULT.name()
            )),
            new HashMap<>(Map.of(
                BusID.MUX_ALUsrc_TO_ALU.name(), String.valueOf(immediate),
                BusID.ALU_TO_MUX_memtoreg_RESULT.name(), String.format("%d", result)
            )),
            () -> {
                if (signals.isFlagWrite() && flagUpdater != null) {
                    flagUpdater.updateFlags(aluResult);
                }
            }
        ));

        // Step 4: Write-Back
        if (signals.isRegWrite()) {
            microStepQueue.add(new MicroStep(
                "Step 4: Write Back",
                PipelineStage.WRITE_BACK,
                new ArrayList<>(List.of("MUX_MEMTOREG", "REGISTERS")),
                new ArrayList<>(List.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name())),
                new HashMap<>(Map.of(
                    BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name(), String.format("%d", result)
                )),
                () -> {
                    registerFile.writeRegister(rd, result, true);
                }
            ));
        } else {
            microStepQueue.add(new MicroStep(
                "Step 4: No Write-Back",
                PipelineStage.WRITE_BACK,
                new ArrayList<>(List.of()),
                new ArrayList<>(List.of()),
                new HashMap<>(Map.of("INFO", String.format("Result: %d (not written to register)", result))),
                null
            ));
        }
    }

    // --- R-Format instruction micro-steps ---
    private void generateRFormatSteps(RFormatInstruction rInst, boolean zeroFlag) {
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
        long operandB = rmValue;

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
                operandB = shamt;
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
                op = ArithmeticLogicUnit.ALUOperation.ADD;
        }

        final ArithmeticLogicUnit.ALUResult aluResult = alu.execute(rnValue, operandB, op);
        final long result = aluResult.result;

        // Step 1: Instruction Fetch
        generateInstructionFetchSteps();

        // Step 2: Decode & Register Read
        if (mnemonic.equals("LSL") || mnemonic.equals("LSR") || mnemonic.equals("ASR")) {
            microStepQueue.add(new MicroStep(
                "Step 2: Decode & Register Read",
                PipelineStage.DECODE,
                new ArrayList<>(List.of("INSTRUCTION_MEMORY", "CONTROL_UNIT", "REGISTERS", "SIGN_EXTEND", "MUX_ALUsrc")),
                new ArrayList<>(List.of(
                    BusID.INSTRUCTION_MEMORY_.name(),
                    BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                    BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), 
                    BusID.INSTRUCTION_MEMORY_TO_REGISTERS_READ1.name(),   
                    BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE.name(),
                    BusID.INSTRUCTION_MEMORY_TO_ALU_CONTROL.name(),             
                    BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(),
                    BusID.INSTRUCTION_MEMORY_TO_ALU_CONTROL.name()
                )),
                new HashMap<>(Map.of(
                    BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), definition.getOpcodeId(),
                    BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), String.format("%32s", Long.toBinaryString(operandB & 0xFFFF)).replace(' ', '0'),
                    BusID.INSTRUCTION_MEMORY_TO_REGISTERS_READ1.name(), String.format("%5s", Integer.toBinaryString(rn & 0x1F)).replace(' ', '0'),
                    BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE.name(), String.format("%5s", Integer.toBinaryString(rd & 0x1F)).replace(' ', '0'),
                    BusID.INSTRUCTION_MEMORY_TO_ALU_CONTROL.name(), generateAluControlSignal(signals.getAluOp(), definition.getOpcodeId()),
                    BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(), String.valueOf(operandB),
                    BusID.INSTRUCTION_MEMORY_TO_ALU_CONTROL.name(), String.format("%10s", Integer.toBinaryString(signals.getAluOp())).replace(' ', '0')
                )),
                null
            ));
        }
        else {
            microStepQueue.add(new MicroStep(
            "Step 2: Decode & Register Read",
            PipelineStage.DECODE,
            new ArrayList<>(List.of("INSTRUCTION_MEMORY", "CONTROL_UNIT", "REGISTERS", "SIGN_EXTEND", "MUX_reg2loc")),
            new ArrayList<>(List.of(
                BusID.INSTRUCTION_MEMORY_.name(),
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), 
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_READ1.name(),   
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE.name(),
                BusID.INSTRUCTION_MEMORY_TO_MUX_reg2loc_0.name(),
                BusID.MUX_reg2loc_TO_REGISTERS_READ2.name()            
            )),
            new HashMap<>(Map.of(
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), String.format("%11s", definition.getOpcodeId()),
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), String.format("%32s", Long.toBinaryString(0).replace(' ', '0')),
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_READ1.name(), String.format("%5s", Integer.toBinaryString(rn & 0x1F)).replace(' ', '0'),
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE.name(), String.format("%5s", Integer.toBinaryString(rd & 0x1F)).replace(' ', '0'),
                BusID.INSTRUCTION_MEMORY_TO_MUX_reg2loc_0.name(), String.format("%5s", Integer.toBinaryString(rm & 0x1F)).replace(' ', '0'),
                BusID.MUX_reg2loc_TO_REGISTERS_READ2.name(), String.valueOf(rmValue)
            )),
        null
        ));
        }
        
        generateControlUnitSteps(signals, definition, zeroFlag);

        // Step 3: Execute (ALU)
        microStepQueue.add(new MicroStep(
            "Step 3: Execute (ALU)",
            PipelineStage.EXECUTE,
            new ArrayList<>(List.of("ALU", "MUX_ALUsrc")),
            new ArrayList<>(List.of(
                BusID.REGISTERS_TO_ALU_READ1.name(), 
                BusID.REGISTERS_TO_MUX_ALUsrc_READ2.name(), 
                BusID.ALU_TO_MUX_memtoreg_RESULT.name(),
                BusID.MUX_ALUsrc_TO_ALU.name())),
            new HashMap<>(Map.of(
                BusID.REGISTERS_TO_ALU_READ1.name(), String.valueOf(rnValue),
                BusID.REGISTERS_TO_MUX_ALUsrc_READ2.name(), String.valueOf(rmValue),
                BusID.ALU_TO_MUX_memtoreg_RESULT.name(), String.valueOf(result),
                BusID.MUX_ALUsrc_TO_ALU.name(), String.valueOf(operandB)
                )),
            () -> {
                if (signals.isFlagWrite() && flagUpdater != null) {
                    flagUpdater.updateFlags(aluResult);
                }
            }
        ));

        // Step 4: Write-Back
        if (signals.isRegWrite()) {
            microStepQueue.add(new MicroStep(
                "Step 4: Write-Back",
                PipelineStage.WRITE_BACK,
                new ArrayList<>(List.of("MUX_memtoreg", "REGISTERS")),
                new ArrayList<>(List.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name())),
                new HashMap<>(Map.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name(), String.valueOf(result))),
                () -> {
                    registerFile.writeRegister(rd, result, true);
                }
            ));
        } else {
            microStepQueue.add(new MicroStep(
                "Step 4: No Write-Back",
                PipelineStage.WRITE_BACK,
                new ArrayList<>(List.of()),
                new ArrayList<>(List.of()),
                new HashMap<>(Map.of("INFO", String.format("Result: %d (not written to register)", result))),
                null
            ));
        }
    }

    // --- B-Format instruction micro-steps ---
    private void generateBFormatSteps(BFormatInstruction bInst, boolean zeroFlag) {
        long offset = bInst.getAddress_B();
        final int targetPc = this.pc + (int) offset;

        InstructionDefinition definition = bInst.getDefinition();
        ControlSignals signals = controlUnit.generateControlSignals(bInst);

        // Determine if branch should be taken
        if (signals.isUncondBranch()) { // Lệnh B hoặc BL
            takeTheBranch = true;
        } else if (signals.isFlagBranch()) { // Lệnh B.cond
            // Extract condition code from mnemonic (e.g., "B.EQ" -> "EQ")
            String mnemonic = definition.getMnemonic();
            String conditionCode = mnemonic.contains(".") ? 
                mnemonic.substring(mnemonic.indexOf('.') + 1) : "EQ";

            // Evaluate condition with current CPU flags
            takeTheBranch = this.cpu.evaluateBranchCondition(conditionCode);
        } else {
            takeTheBranch = false;
        }

        // Step 1: Instruction Fetch
        generateInstructionFetchSteps();

        // Step 2: Decode & Calculate Branch Target
        microStepQueue.add(new MicroStep(
            "Step 2: Decode & Calculate Branch Target",
            PipelineStage.DECODE,
            new ArrayList<>(List.of("CONTROL_UNIT", "SIGN_EXTEND", "ADD_BRANCH", "SHIFT_LEFT_2")),
            new ArrayList<>(List.of(BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                    BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), 
                    BusID.SIGN_EXTEND_TO_SHIFT_LEFT_2.name(), BusID.PC_TO_ADD_2.name(), 
                    BusID.SHIFT_LEFT_2_TO_ADD.name(),
                    BusID.ADD_2_TO_MUX_PCSRC.name())),
            new HashMap<>(Map.of(
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), definition.getOpcodeId(),
                BusID.PC_TO_ADD_2.name(), String.format("0x%X", this.pc),
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), String.format("%32s", Long.toBinaryString(offset & 0x1FFFFFF)).replace(' ', '0'),
                BusID.SIGN_EXTEND_TO_SHIFT_LEFT_2.name(), String.valueOf(offset),
                BusID.ADD_2_TO_MUX_PCSRC.name(), String.format("0x%X", targetPc)
            )),
            null
        ));

        // Step 3: Control Signal Generation with Branch Logic
        generateControlUnitSteps(signals, definition, zeroFlag);

        // Step 4: Execute Branch Logic
        if (takeTheBranch) {
            // Branch taken - update PC to target address
            long targetAddress = (this.pc * 4) + (offset * 4); // Calculate byte address
            
            microStepQueue.add(new MicroStep(
                "Step 4: Execute (Branch Taken)",
                PipelineStage.EXECUTE,
                new ArrayList<>(List.of("SIGN_EXTEND", "SHIFT_LEFT_2", "ADD_2", "MUX_PCSRC", "PC", "OR_GATE")),
                new ArrayList<>(List.of(
                    BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(),
                    BusID.SIGN_EXTEND_TO_SHIFT_LEFT_2.name(),
                    BusID.SHIFT_LEFT_2_TO_ADD.name(),
                    BusID.ADD_2_TO_MUX_PCSRC.name(),
                    BusID.OR_GATE_TO_MUX_PCSRC.name(),
                    BusID.MUX_PCSRC_TO_PC.name()
                )),
                new HashMap<>(Map.of(
                    "INFO", "Condition MET. Branch will be taken.",
                    "Branch Offset", String.valueOf(offset),
                    BusID.ADD_2_TO_MUX_PCSRC.name(), String.format("0x%X", targetAddress),
                    BusID.OR_GATE_TO_MUX_PCSRC.name(), "1",
                    BusID.MUX_PCSRC_TO_PC.name(), String.format("0x%X", targetAddress)
                )),
                () -> {
                    // Update PC will be handled by the calling code
                    System.out.println("Branch taken to address: 0x" + Long.toHexString(targetAddress));
                }
            ));
        } else {
            // Branch not taken - PC increments normally
            microStepQueue.add(new MicroStep(
                "Step 4: Execute (Branch Not Taken)",
                PipelineStage.EXECUTE,
                new ArrayList<>(List.of("CONTROL_UNIT", "MUX_PCSRC", "OR_GATE")),
                new ArrayList<>(List.of(BusID.OR_GATE_TO_MUX_PCSRC.name(), BusID.MUX_PCSRC_TO_PC.name())),
                new HashMap<>(Map.of(
                    "INFO", "Condition NOT met. Branch will not be taken.",
                    BusID.OR_GATE_TO_MUX_PCSRC.name(), "0",
                    BusID.MUX_PCSRC_TO_PC.name(), String.format("0x%X", (pc + 1) * 4)
                )),
                () -> {
                    System.out.println("Branch not taken, PC increments normally");
                }
            ));
        }
    }
    
    // --- D-Format instruction micro-steps ---
    private void generateDFormatSteps(DFormatInstruction dInst, boolean zeroFlag) {
        int rt = dInst.getRt_D();
        int rn = dInst.getRn_D();
        int rawImm = dInst.getAddress_D();
        // Sign-extend the 9-bit immediate value
        long imm = (rawImm & 0x100) != 0 ? (rawImm | 0xFFFFFFFFFFFFFE00L) : rawImm;
        long rnValue = registerFile.readRegister(rn);
        long rtValue = registerFile.readRegister(rt);
        
        // Debug: Log the address calculation components
        System.out.printf("D-format address calculation: X%d=0x%X + imm=%d (0x%X) = 0x%X%n", 
            rn, rnValue, imm, imm & 0xFFFFFFFFL, rnValue + imm);
            
        long address = validateMemoryAddress(rnValue + imm);
        InstructionDefinition definition = dInst.getDefinition();
        ControlSignals signals = controlUnit.generateControlSignals(dInst);

        // Step 1: Instruction Fetch
        generateInstructionFetchSteps();

        // Step 2: Decode & Register Read
        microStepQueue.add(new MicroStep(
            "Step 2: Decode & Register Read",
            PipelineStage.DECODE,
            new ArrayList<>(List.of("INSTRUCTION_MEMORY", "CONTROL_UNIT", "REGISTERS", "SIGN_EXTEND", "MUX_reg2loc")),
            new ArrayList<>(List.of(
                BusID.INSTRUCTION_MEMORY_.name(),
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(),
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_READ1.name(),
                BusID.INSTRUCTION_MEMORY_TO_MUX_reg2loc_1.name(),
                BusID.MUX_reg2loc_TO_REGISTERS_READ2.name(),
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE.name()
            )),
            new HashMap<>(Map.of(
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), definition.getOpcodeId(),
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), String.format("%32s", Long.toBinaryString(rawImm & 0x1FF)).replace(' ', '0'),
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_READ1.name(), String.format("%5s", Integer.toBinaryString(rn & 0x1F)).replace(' ', '0'),
                BusID.INSTRUCTION_MEMORY_TO_MUX_reg2loc_1.name(), String.format("%5s", Integer.toBinaryString(rt & 0x1F)).replace(' ', '0'),
                BusID.MUX_reg2loc_TO_REGISTERS_READ2.name(), String.valueOf(dInst.isLoad() ? "0" : rtValue),
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE.name(), String.format("%5s", Integer.toBinaryString(rt & 0x1F)).replace(' ', '0')
            )),
            null
        ));

        // Step 3: Control Signal Generation
        generateControlUnitSteps(signals, definition, zeroFlag);

        // Step 4: Address Calculation (ALU)
        microStepQueue.add(new MicroStep(
            "Step 4: Address Calculation",
            PipelineStage.EXECUTE,
            new ArrayList<>(List.of("ALU", "SIGN_EXTEND", "MUX_ALUsrc", "REGISTERS")),
            new ArrayList<>(List.of(
                BusID.REGISTERS_TO_ALU_READ1.name(),
                BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(),
                BusID.MUX_ALUsrc_TO_ALU.name(),
                BusID.ALU_TO_DATA_MEMORY_ADDRESS.name()
            )),
            new HashMap<>(Map.of(
                BusID.REGISTERS_TO_ALU_READ1.name(), String.valueOf(rnValue),
                BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(), String.valueOf(imm),
                BusID.MUX_ALUsrc_TO_ALU.name(), String.valueOf(imm),
                BusID.ALU_TO_DATA_MEMORY_ADDRESS.name(), String.format("0x%X", address)
            )),
            null
        ));

        // Step 5: Memory Access
        if (dInst.isLoad()) { // Case for LDUR
            microStepQueue.add(new MicroStep(
                "Step 5: Memory Access (Read)",
                PipelineStage.MEMORY_ACCESS,
                new ArrayList<>(List.of("DATA_MEMORY")),
                new ArrayList<>(List.of(
                    BusID.ALU_TO_DATA_MEMORY_ADDRESS.name(),
                    BusID.DATA_MEMORY_TO_MUX_memtoreg_READ.name()
                )),
                new HashMap<>(Map.of(
                    BusID.ALU_TO_DATA_MEMORY_ADDRESS.name(), String.format("0x%X", address),
                    BusID.DATA_MEMORY_TO_MUX_memtoreg_READ.name(), String.format("0x%X", memory.read(address, 8))
                )),
                null
            ));

            // Step 6: Write-Back (for LDUR)
            microStepQueue.add(new MicroStep(
                "Step 6: Write-Back",
                PipelineStage.WRITE_BACK,
                new ArrayList<>(List.of("DATA_MEMORY", "MUX_memtoreg", "REGISTERS")),
                new ArrayList<>(List.of(
                    BusID.DATA_MEMORY_TO_MUX_memtoreg_READ.name(),
                    BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name()
                )),
                new HashMap<>(Map.of(
                    BusID.DATA_MEMORY_TO_MUX_memtoreg_READ.name(), String.format("0x%X", memory.read(address, 8)),
                    BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name(), String.format("0x%X", memory.read(address, 8))
                )),
                () -> registerFile.writeRegister(rt, memory.read(address, 8), true)
            ));

        } else { // Case for STUR
            microStepQueue.add(new MicroStep(
                "Step 5: Memory Access (Write)",
                PipelineStage.MEMORY_ACCESS,
                new ArrayList<>(List.of("REGISTERS", "DATA_MEMORY")),
                new ArrayList<>(List.of(
                    BusID.ALU_TO_DATA_MEMORY_ADDRESS.name(),
                    BusID.REGISTERS_TO_DATA_MEMORY_WRITE_DATA.name()
                )),
                new HashMap<>(Map.of(
                    BusID.ALU_TO_DATA_MEMORY_ADDRESS.name(), String.format("0x%X", address),
                    BusID.REGISTERS_TO_DATA_MEMORY_WRITE_DATA.name(), String.format("0x%X", rtValue)
                )),
                () -> memory.write(address, rtValue, 8)
            ));
            
            // Step 6: No Write-Back for STUR
            microStepQueue.add(new MicroStep(
                "Step 6: No Write-Back",
                PipelineStage.WRITE_BACK,
                new ArrayList<>(List.of()),
                new ArrayList<>(List.of()),
                new HashMap<>(Map.of("INFO", "STUR does not write to a register.")),
                null
            ));
        }
    }

    // --- IM-Format instruction micro-steps ---
    private void generateIMFormatSteps(IMFormatInstruction imInst, boolean zeroFlag) {
        int rd = imInst.getRd_IM();
        int hw = imInst.getShift_IM();
        long imm16 = imInst.getImmediate_IM();
        int shiftAmount = hw * 16;
        final long result = imm16 << shiftAmount;
        int rn = imInst.getRn_IM();

        ControlSignals signals = controlUnit.generateControlSignals(imInst);
        InstructionDefinition definition = imInst.getDefinition();

        // Step 1: Instruction Fetch
        generateInstructionFetchSteps();

        // Step 2: Decode
        microStepQueue.add(new MicroStep(
            "Step 2: Decode & Register Read",
            PipelineStage.DECODE,
            new ArrayList<>(List.of("INSTRUCTION_MEMORY", "CONTROL_UNIT", "REGISTERS", "SIGN_EXTEND", "MUX_ALUsrc")),
            new ArrayList<>(List.of(
                BusID.INSTRUCTION_MEMORY_.name(),
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), 
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), 
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_READ1.name(),   
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE.name(),
                BusID.INSTRUCTION_MEMORY_TO_ALU_CONTROL.name(),             
                BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name()
            )),
            new HashMap<>(Map.of(
                BusID.INSTRUCTION_MEMORY_TO_CONTROL_UNIT.name(), definition.getOpcodeId(),
                BusID.INSTRUCTION_MEMORY_TO_SIGN_EXTEND.name(), String.format("%32s", Long.toBinaryString(imm16 & 0xFFFF)).replace(' ', '0'),
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_READ1.name(), String.format("%5s", Integer.toBinaryString(rn & 0x1F)).replace(' ', '0'),
                BusID.INSTRUCTION_MEMORY_TO_REGISTERS_WRITE.name(), String.format("%5s", Integer.toBinaryString(rd & 0x1F)).replace(' ', '0'),
                BusID.INSTRUCTION_MEMORY_TO_ALU_CONTROL.name(), generateAluControlSignal(signals.getAluOp(), definition.getOpcodeId()),
                BusID.SIGN_EXTEND_TO_MUX_ALUsrc.name(), String.valueOf(imm16)
            )),
            null
        ));
        
        generateControlUnitSteps(signals, definition, zeroFlag);

        // Step 3: Execute
        microStepQueue.add(new MicroStep(
            "Step 3: Execute",
            PipelineStage.EXECUTE,
            new ArrayList<>(List.of("ALU", "MUX_memtoreg")),
            new ArrayList<>(List.of(
                BusID.MUX_ALUsrc_TO_ALU.name(),
                BusID.ALU_TO_MUX_memtoreg_RESULT.name())),
            new HashMap<>(Map.of(
                BusID.ALU_TO_MUX_memtoreg_RESULT.name(), String.format("0x%X", result),
                BusID.MUX_ALUsrc_TO_ALU.name(), String.format("0x%X", (imm16 << shiftAmount))
            )),
            null
        ));
        
        // Step 4: Write-Back
        microStepQueue.add(new MicroStep(
            "Step 4: Write-Back",
            PipelineStage.WRITE_BACK,
            new ArrayList<>(List.of("MUX_memtoreg", "REGISTERS")),
            new ArrayList<>(List.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name())),
            new HashMap<>(Map.of(BusID.MUX_memtoreg_TO_REGISTERS_WRITE.name(), String.valueOf(result))),
            () -> registerFile.writeRegister(rd, result, true)
        ));
    }
    
    // --- ALU Control Signal Generation ---
    private String generateAluControlSignal(int aluOp, String opcodeId) {
        switch (aluOp) {
            case 0: return "0010"; // ADD for memory operations
            case 1: return "0110"; // SUB for branch operations  
            case 2: return mapInstructionToAluControl(opcodeId); // R-type and I-type
            default: return "0000"; // Default/unknown
        }
    }

    private String mapInstructionToAluControl(String opcodeId) {
        switch (opcodeId) {
            case "1001000100": return "0010"; // ADDI
            case "1101000100": return "0110"; // SUBI
            case "10001011000": return "0010"; // ADD
            case "11001011000": return "0110"; // SUB
            case "10001010000": return "0000"; // AND
            case "10101010000": return "0001"; // ORR
            case "11001010000": return "1100"; // EOR
            case "10011011000": return "1101"; // MUL
            case "11001011010": return "0110"; // CMP
            case "10001010110": return "1110"; // SDIV
            case "10001011010": return "1111"; // UDIV
            case "11010011011": return "0011"; // LSL
            case "11010011010": return "0100"; // LSR
            case "11010011001": return "0101"; // ASR
            case "10011011010": return "1000"; // SMULH
            case "10011011110": return "1001"; // UMULH
            default: return "0010"; // Default to ADD
        }
    }

    /**
     * Validates that a memory address is within the valid bounds for this simulator.
     * @param address The address to validate
     * @return The validated address, or throws exception if invalid
     * @throws MemoryAccessException if address is out of bounds
     */
    private long validateMemoryAddress(long address) {
        // Ensure address is within the valid memory range (0 to MEMORY_SIZE-1)
        // Also handle the case where address might be negative due to sign extension
        if (address < 0) {
            throw new MemoryAccessException("Negative memory address not allowed: 0x" + Long.toHexString(address) + 
                " (Memory range: 0x0 to 0x" + Long.toHexString(Constants.MEMORY_SIZE - 1) + ")");
        }
        if (address >= Constants.MEMORY_SIZE) {
            throw new MemoryAccessException("Memory address out of bounds: 0x" + Long.toHexString(address) + 
                " (Memory range: 0x0 to 0x" + Long.toHexString(Constants.MEMORY_SIZE - 1) + ")");
        }
        return address;
    }
    
    /**
     * Evaluates branch condition based on current CPU flags.
     * This method mirrors the implementation in CPUSimulator.
     */
    public boolean evaluateBranchCondition(String condition) {
        switch (condition.toUpperCase()) {
            case "EQ": return zeroFlag;
            case "NE": return !zeroFlag;
            case "LT": return negativeFlag != overflowFlag;
            case "GT": return !zeroFlag && (negativeFlag == overflowFlag);
            case "LE": return zeroFlag || (negativeFlag != overflowFlag);
            case "GE": return negativeFlag == overflowFlag;
            default: return false;
        }
    }

    /**
     * Gets whether the current branch instruction should be taken
     */
    public boolean isBranchTaken() {
        return takeTheBranch;
    }
    
    /**
     * Sets the branch taken status for the current instruction
     */
    public void setBranchTaken(boolean taken) {
        this.takeTheBranch = taken;
    }
}
