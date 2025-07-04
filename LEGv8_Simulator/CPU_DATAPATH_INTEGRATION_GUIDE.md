// CPU SIMULATOR AND DATAPATH PANEL INTEGRATION GUIDE
// How to integrate the execution history system between CPUSimulator and DatapathPanel

/*
INTEGRATION PATTERN FOR EXECUTION HISTORY

1. UPDATE YOUR GUI CLASS TO INTEGRATE BOTH COMPONENTS:

public class LEGv8GUI {
    private CPUSimulator cpuSimulator;
    private DatapathPanel datapathPanel;
    private JButton stepButton;
    private JButton stepBackButton;
    private JButton stepForwardButton;
    
    public void initializeComponents() {
        // Initialize CPU simulator and datapath panel
        InstructionConfigLoader configLoader = new InstructionConfigLoader();
        cpuSimulator = new CPUSimulator(configLoader);
        datapathPanel = new DatapathPanel();
        
        // Setup buttons
        stepButton = new JButton("Step");
        stepBackButton = new JButton("Step Back");
        stepForwardButton = new JButton("Step Forward");
        
        // Add action listeners
        stepButton.addActionListener(e -> performStep());
        stepBackButton.addActionListener(e -> performStepBack());
        stepForwardButton.addActionListener(e -> performStepForward());
        
        // Initial button states
        updateButtonStates();
    }
    
    private void performStep() {
        // Execute one step in CPU simulator
        cpuSimulator.step();
        
        // Update datapath panel with current state
        updateDatapathVisualization();
        
        // Update button states
        updateButtonStates();
    }
    
    private void performStepBack() {
        if (cpuSimulator.canStepBack()) {
            boolean success = cpuSimulator.stepBack();
            if (success) {
                // Update datapath panel to show restored state
                updateDatapathVisualization();
                updateButtonStates();
                
                // Optional: Update register/memory displays
                updateRegisterDisplay();
                updateMemoryDisplay();
            }
        }
    }
    
    private void performStepForward() {
        if (cpuSimulator.canStepForward()) {
            boolean success = cpuSimulator.stepForward();
            if (success) {
                updateDatapathVisualization();
                updateButtonStates();
                updateRegisterDisplay();
                updateMemoryDisplay();
            }
        }
    }
    
    private void updateDatapathVisualization() {
        // Update register values in datapath panel
        for (int i = 0; i < 32; i++) {
            long regValue = cpuSimulator.getRegisterValue(i);
            datapathPanel.updateRegisterValue(i, regValue);
        }
        
        // Update memory state
        Map<Long, Long> memoryState = cpuSimulator.getMemoryState();
        for (Map.Entry<Long, Long> entry : memoryState.entrySet()) {
            datapathPanel.updateMemoryValue(entry.getKey(), entry.getValue());
        }
        
        // Update visualization components and buses
        datapathPanel.setActiveComponentsAndBuses(
            cpuSimulator.getActiveComponents(),
            cpuSimulator.getActiveBuses(),
            cpuSimulator.getBusDataValues()
        );
        
        // Record state in datapath panel's history
        boolean[] flags = {
            cpuSimulator.isZeroFlag(),
            cpuSimulator.isNegativeFlag(),
            cpuSimulator.isOverflowFlag(),
            cpuSimulator.isCarryFlag()
        };
        
        datapathPanel.recordExecutionState(
            cpuSimulator.getCurrentHistoryStepDescription(),
            cpuSimulator.getPc(),
            flags,
            cpuSimulator.getLastExecutedInstruction(),
            cpuSimulator.isFinished(),
            0 // micro-step index
        );
    }
    
    private void updateButtonStates() {
        stepBackButton.setEnabled(cpuSimulator.canStepBack());
        stepForwardButton.setEnabled(cpuSimulator.canStepForward());
        stepButton.setEnabled(!cpuSimulator.isFinished());
    }
    
    private void updateRegisterDisplay() {
        // Update your register display component
        for (int i = 0; i < 32; i++) {
            long value = cpuSimulator.getRegisterValue(i);
            // Update register display UI
            // registerDisplayPanel.updateRegister(i, value);
        }
    }
    
    private void updateMemoryDisplay() {
        // Update your memory display component
        Map<Long, Long> memoryState = cpuSimulator.getMemoryState();
        // memoryDisplayPanel.updateMemoryState(memoryState);
    }
    
    public void loadProgram(String[] assemblyLines) {
        cpuSimulator.loadProgram(assemblyLines);
        updateDatapathVisualization(); // Show initial state
        updateButtonStates();
    }
    
    public void resetProgram() {
        cpuSimulator.reset();
        datapathPanel.clearHistory();
        updateDatapathVisualization();
        updateButtonStates();
    }
}

2. ALTERNATIVE: CREATE A CONTROLLER CLASS

public class ExecutionController {
    private CPUSimulator cpu;
    private DatapathPanel datapath;
    
    public ExecutionController(CPUSimulator cpu, DatapathPanel datapath) {
        this.cpu = cpu;
        this.datapath = datapath;
    }
    
    public void step() {
        cpu.step();
        syncDatapathWithCPU();
    }
    
    public boolean stepBack() {
        boolean success = cpu.stepBack();
        if (success) {
            syncDatapathWithCPU();
        }
        return success;
    }
    
    public boolean stepForward() {
        boolean success = cpu.stepForward();
        if (success) {
            syncDatapathWithCPU();
        }
        return success;
    }
    
    private void syncDatapathWithCPU() {
        // Update all datapath state from CPU state
        // (same as updateDatapathVisualization above)
    }
    
    public boolean canStepBack() { return cpu.canStepBack(); }
    public boolean canStepForward() { return cpu.canStepForward(); }
}

3. KEY INTEGRATION POINTS:

- Always call updateDatapathVisualization() after CPU state changes
- Use CPUSimulator.canStepBack() and canStepForward() for button states
- CPUSimulator handles all execution state and history automatically
- DatapathPanel provides visualization and can maintain its own history
- Synchronize both histories for consistent step-back behavior

4. DEBUGGING FEATURES:

// Get execution statistics
String stats = cpuSimulator.getExecutionStatistics();
System.out.println(stats);

// Get current step description
String currentStep = cpuSimulator.getCurrentHistoryStepDescription();

// Clear history when needed
cpuSimulator.clearExecutionHistory();
datapathPanel.clearHistory();

5. ADVANCED FEATURES:

// Jump to specific state (if implemented)
ExecutionHistory history = cpuSimulator.getExecutionHistory();
ExecutionState specificState = history.jumpToState(10);

// Find specific steps
List<Integer> addInstructions = history.findStatesByDescription("ADD");

This integration provides:
- Full step-back/step-forward functionality
- Synchronized CPU and visualization state
- Complete execution history tracking
- Easy GUI integration with button state management
*/
