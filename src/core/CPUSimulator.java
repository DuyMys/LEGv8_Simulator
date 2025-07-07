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
    private MicroStepManager microStepManager;
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
        this.microStepManager = new MicroStepManager(registerFile, alu, controlUnit, memory, this::updateFlags);
        
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
        
        // Use the new label-aware instruction creation method
        List<Instruction> instructions = factory.createFromAssemblyLines(assemblyLines);
        program.addAll(instructions);
        
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
     * Gets the most recently executed (or restored) execution state from history.
     * This provides a stable view of the current state for the GUI, avoiding timing issues.
     * @return The current ExecutionState, or null if history is empty.
     */
    public ExecutionState getCurrentExecutionState() {
        return executionHistory.getCurrentState();
    }

    public void step() {
        if (isFinished) return;

        // If no micro-steps are available, it means we are starting a new instruction.
        // This is the CORRECT place to generate them.
        if (microStepManager.isEmpty()) {
            // Check if the program is over
            if (pc >= program.size()) {
                isFinished = true;
                lastExecutedInstruction = "Execution Complete";
                clearDatapathActivity();
                
                // Record a final, single "finished" state to history
                if (!isRestoringFromHistory) {
                    // We create a special state with an invalid micro-step index
                    // to signal completion.
                    ExecutionState finalState = new ExecutionState(pc, zeroFlag, negativeFlag, overflowFlag, carryFlag, 
                                                                "Execution Complete", true, -1, 
                                                                // Pass current register/memory state
                                                                registerFile.getAllRegisters(), memory.getAllData(), 
                                                                activeComponents, activeBuses, busDataValues, 
                                                                "Program execution completed");
                    executionHistory.addState(finalState);
                }
                return;
            }
            
            // It's a new instruction, so let's set it up.
            Instruction instruction = program.get(pc);
            lastExecutedInstruction = instruction.disassemble();
            this.branchTaken = false; 
            
            microStepManager.updateCPUState(pc, zeroFlag, negativeFlag, overflowFlag, carryFlag, branchTaken);
            microStepManager.generateMicroStepsFor(instruction, zeroFlag);
        }
        
        // Now, we are guaranteed to have a micro-step to execute.
        MicroStep currentStep = microStepManager.getCurrentMicroStep();
        if (currentStep != null) {
            // Set visualization state for the GUI
            this.activeComponents = currentStep.getActiveComponents();
            this.activeBuses = currentStep.getActiveBuses();
            this.busDataValues = currentStep.getBusDataValues();
            
            // Record state to history BEFORE executing the action
            if (!isRestoringFromHistory) {
                String stepDescription = String.format("Micro-step %d/%d: %s - %s", 
                    microStepManager.getCurrentMicroStepIndex() + 1, microStepManager.getTotalMicroSteps(),
                    lastExecutedInstruction, currentStep.getDescription());
                recordCurrentStateToHistory(stepDescription);
            }
            
            // Execute the action
            if (currentStep.getAction() != null) {
                currentStep.getAction().run();
                // ... (handle branch/ALU flags if necessary) ...
            }

            // Advance to the next micro-step for the NEXT call to step()
            microStepManager.advanceToNextMicroStep();
        }
        
        // If we have now run out of micro-steps, reset the manager
        // and update the PC for the next full instruction.
        if (microStepManager.isEmpty()) { // This now checks if we've gone past the end
            // Update PC for the next instruction
            if (!branchTaken) {
                pc++;
            }
            printState();
        }
    }

    public void reset() {
        pc = 0;
        registerFile.reset();
        memory.reset();
        isFinished = false;
        branchTaken = false;
        microStepManager.reset();
        clearDatapathActivity();
        lastExecutedInstruction = "None";
        zeroFlag = false;
        negativeFlag = false;
        overflowFlag = false;
        carryFlag = false;
        
        // Clear control unit state
        if (controlUnit != null) {
            controlUnit.clearControlSignals();
        }
        
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
        
        // Create execution state with proper micro-step context
        ExecutionState state = new ExecutionState(
            pc,
            zeroFlag,
            negativeFlag,
            overflowFlag,
            carryFlag,
            lastExecutedInstruction,
            isFinished,
            microStepManager.getCurrentMicroStepIndex(),
            registerValues,
            memoryValues,
            new ArrayList<>(activeComponents),
            new ArrayList<>(activeBuses),
            new HashMap<>(busDataValues),
            stepDescription
        );
        
        executionHistory.addState(state);
        
        // Debug output to help track history recording
        System.out.println("DEBUG: Recorded state - PC: " + pc + ", MicroStep: " + microStepManager.getCurrentMicroStepIndex() + 
                          "/" + microStepManager.getTotalMicroSteps() + ", Description: " + stepDescription);
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
        
        System.out.println("DEBUG: Stepping back to: " + previousState.getStepDescription());
        restoreFromState(previousState);
        
        // Ensure control unit state is properly restored if needed
        if (controlUnit != null && previousState.getStepDescription().contains("Control Signal")) {
            try {
                // Try to restore control unit state based on current instruction
                if (pc < program.size()) {
                    Instruction currentInstr = program.get(pc);
                    ControlSignals signals = controlUnit.generateControlSignals(currentInstr);
                    controlUnit.setControlSignals(signals);
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not restore control unit state: " + e.getMessage());
            }
        }
        
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
        
        System.out.println("DEBUG: Stepping forward to: " + nextState.getStepDescription());
        restoreFromState(nextState);
        
        // Ensure control unit state is properly restored if needed
        if (controlUnit != null && nextState.getStepDescription().contains("Control Signal")) {
            try {
                // Try to restore control unit state based on current instruction
                if (pc < program.size()) {
                    Instruction currentInstr = program.get(pc);
                    ControlSignals signals = controlUnit.generateControlSignals(currentInstr);
                    controlUnit.setControlSignals(signals);
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not restore control unit state: " + e.getMessage());
            }
        }
        
        return true;
    }

    private void restoreFromState(ExecutionState state) {
        // ... (restore all fields like pc, flags, registers, memory as before) ...
        this.pc = state.getProgramCounter();
        this.zeroFlag = state.isZeroFlag();
        this.negativeFlag = state.isNegativeFlag();
        this.overflowFlag = state.isOverflowFlag();
        this.carryFlag = state.isCarryFlag();
        this.lastExecutedInstruction = state.getLastExecutedInstruction();
        this.isFinished = state.isFinished();
        
        // ... restore register and memory files ...
        registerFile.reset();
        for (Map.Entry<Integer, Long> entry : state.getRegisterValues().entrySet()) {
            registerFile.writeRegister(entry.getKey(), entry.getValue(), false);
        }
        
        memory.reset();
        for (Map.Entry<Long, Long> entry : state.getModifiedMemoryValues().entrySet()) {
            memory.write(entry.getKey(), entry.getValue(), 8);
        }
        
        this.activeComponents = new ArrayList<>(state.getActiveComponents());
        this.activeBuses = new ArrayList<>(state.getActiveBuses());
        this.busDataValues = new HashMap<>(state.getBusDataValues());

        // --- MODIFIED LOGIC IS HERE ---

        // Check if we are restoring to the special initial state.
        // We can identify it by its description or by checking if the micro-step index is 0
        // AND it's the very first state in the history. A description check is more robust.
        if (state.getStepDescription().contains("Initial state")) {
            // This is the "before anything happens" state. Do NOT generate micro-steps.
            // Just reset the manager to a clean slate.
            microStepManager.reset();
            System.out.println("DEBUG: Restored to initial program state. Micro-step manager reset.");
        } else if (!isFinished && pc < program.size()) {
            // This is the normal case for restoring to the middle of an instruction.
            Instruction currentInstruction = program.get(pc);
            if (currentInstruction != null) {
                // Generate the full micro-step sequence for this instruction
                microStepManager.generateMicroStepsFor(currentInstruction, this.zeroFlag); 
                
                // Now, set the manager's index to the one we restored from the state object.
                microStepManager.setCurrentMicroStepIndex(state.getCurrentMicroStepIndex());
                
                System.out.println("DEBUG: Regenerated micro-steps for: " + currentInstruction.disassemble() + 
                                " and set index to " + state.getCurrentMicroStepIndex());
            }
        } else {
            // This handles the "Execution Complete" state or other edge cases.
            microStepManager.reset();
        }
        
        System.out.println("State restored: " + state.getStepDescription());
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
        return microStepManager.getCurrentMicroStepDescription();
    }
    public String getLastExecutedInstruction() { return lastExecutedInstruction; }
    public int getInstructionCount() { return program.size(); }
    public boolean isZeroFlag() { return zeroFlag; }
    public boolean isNegativeFlag() { return negativeFlag; }
    public boolean isOverflowFlag() { return overflowFlag; }
    public boolean isCarryFlag() { return carryFlag; }
    public List<Instruction> getProgram() { return program; }
    public RegisterFileController getRegisterFile() { return registerFile; }
    public MicroStepManager getMicroStepManager() { return microStepManager; }
    
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
        microStepManager.setCurrentMicroStepIndex(index);
    }
    
    /**
     * Sets the current micro-step description for GUI updates.
     * This method is now handled by MicroStepManager.
     */
    public void setCurrentMicroStep(String stepDescription) {
        // Description is now handled by MicroStepManager
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
        return microStepManager.getCurrentMicroStepIndex();
    }
    
    public int getTotalMicroSteps() {
        return microStepManager.getTotalMicroSteps();
    }
    
    public boolean hasMoreMicroSteps() {
        return microStepManager.hasMoreMicroSteps();
    }
    
    // Flag getter methods
    public boolean getZeroFlag() {
        return zeroFlag;
    }
    
    public boolean getNegativeFlag() {
        return negativeFlag;
    }
    
    public boolean getCarryFlag() {
        return carryFlag;
    }
    
    public boolean getOverflowFlag() {
        return overflowFlag;
    }

    /**
     * Handles branch execution and updates PC
     */
    private void handleBranchExecution(MicroStep currentStep) {
        if (currentStep.getDescription().contains("Branch (Update PC)")) {
            // Extract target PC from bus data
            String pcValue = currentStep.getBusDataValues().get(BusID.MUX_PCSRC_TO_PC.name());
            if (pcValue != null) {
                try {
                    // Convert from address to PC (divide by 4)
                    int targetAddress = Integer.parseInt(pcValue.replace("0x", ""), 16);
                    this.pc = targetAddress / 4;
                    this.branchTaken = true;
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing branch target: " + pcValue);
                }
            }
        }
    }
    /**
     * Gets the pipeline stage of the current micro-step.
     * This is used by the GUI to highlight the correct part of the datapath.
     * @return The current PipelineStage, or PipelineStage.NONE if no step is active.
     */
    public PipelineStage getCurrentPipelineStage() {
        if (isFinished) {
            return PipelineStage.NONE;
        }
        MicroStep currentStep = microStepManager.getCurrentMicroStep();
        if (currentStep != null) {
            return currentStep.getStage();
        }
        return PipelineStage.NONE; // Default if between instructions or before start
    }
    
    /**
     * Handles ALU execution and flag updates
     */
    private void handleALUExecution() {
        // Flag updates are handled within the micro-step actions in MicroStepManager
        // This method is kept for future extensions if needed
    }

    /**
     * Evaluates a branch condition based on current flags.
     * @param condition The condition code (e.g., "EQ", "NE", "LT", "GT", "LE", "GE")
     * @return true if the condition is satisfied based on current flags, false otherwise.
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

}
