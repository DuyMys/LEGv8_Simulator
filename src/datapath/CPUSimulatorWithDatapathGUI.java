package datapath;

import instruction.Instruction;
import instruction.InstructionFactory;
import instruction.InstructionConfigLoader;
import instruction.IFormatInstruction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Timer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CPUSimulatorWithDatapathGUI extends JFrame {
    private JTextArea codeTextArea;
    private JTextArea outputTextArea;
    private DatapathPanel datapathPanel;
    private JButton stepButton;
    private JButton animateButton;
    private JButton resetButton; 

    private JButton stepBackButton;
    private List<MicroStep> currentMicroSteps;
    private int currentMicroStepIndex;
    
    private Timer animationTimer;
    private List<String> program;
    private long pc; // Program counter (byte address)
    private long[] registers; // 32 registers (X0-X31)
    private Map<String, Long> labelMap; // Maps labels to byte addresses
    private InstructionFactory instructionFactory;
    private static final int ANIMATION_DELAY = 5000; // 5 seconds per step
    private static final long INSTRUCTION_SIZE = 4; // Bytes per instruction
    private List<StateSnapshot> stateHistory; // New: Stores CPU states for step back
    private static final String MEMORY_FILE = "memory_log.txt"; 

    public CPUSimulatorWithDatapathGUI() {
        initGUI();
        registers = new long[32];
        labelMap = new HashMap<>();
        pc = 0x10000;
        currentMicroSteps = new ArrayList<>();
        currentMicroStepIndex = 0;
        stateHistory = new ArrayList<>(); // Initialize state history
        instructionFactory = new InstructionFactory(new InstructionConfigLoader());
        initializeMemoryFile(); // Initialize memory file
    }
    private void initializeMemoryFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(MEMORY_FILE, false))) {
            writer.println("CPU Memory Log - Initialized at " + getCurrentTimestamp());
            writer.println("----------------------------------------");
        } catch (IOException e) {
            outputTextArea.append("Error initializing memory file: " + e.getMessage() + "\n");
        }
    }
    private String getCurrentTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }
    private void logInstructionResult(String instruction) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(MEMORY_FILE, true))) {
            writer.println("Timestamp: " + getCurrentTimestamp());
            writer.println("Instruction: " + instruction);
            writer.println("PC: 0x" + Long.toHexString(pc));
            writer.println("Registers Modified:");
            for (int i = 0; i < registers.length; i++) {
                if (registers[i] != 0) { // Log non-zero registers
                    writer.println("X" + i + ": " + registers[i]);
                }
            }
            writer.println("----------------------------------------");
        } catch (IOException e) {
            outputTextArea.append("Error writing to memory file: " + e.getMessage() + "\n");
        }
    }

    private void initGUI() {
        setTitle("LEGv8 CPU Simulator with Datapath");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Code input panel
        JPanel codePanel = new JPanel(new BorderLayout());
        codePanel.add(new JLabel("Enter LEGv8 Code:"), BorderLayout.NORTH);
        codeTextArea = new JTextArea(10, 15);
        codePanel.add(new JScrollPane(codeTextArea), BorderLayout.CENTER);
        add(codePanel, BorderLayout.WEST);

        // Output panel
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.add(new JLabel("Output:"), BorderLayout.NORTH);
        outputTextArea = new JTextArea(10, 30);
        outputTextArea.setEditable(false);
        outputPanel.add(new JScrollPane(outputTextArea), BorderLayout.CENTER);
        add(outputPanel, BorderLayout.EAST);

        // Datapath panel
        datapathPanel = new DatapathPanel();
        JScrollPane datapathScroll = new JScrollPane(datapathPanel);
        add(datapathScroll, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = new JPanel();
        stepButton = new JButton("Step");
        animateButton = new JButton("Animate");
        resetButton = new JButton("Reset"); 
        stepBackButton = new JButton("Step Back"); 
        controlPanel.add(stepButton);
        controlPanel.add(animateButton);
        controlPanel.add(resetButton);
        controlPanel.add(stepBackButton);
        add(controlPanel, BorderLayout.SOUTH);

        // Action listeners
        stepButton.addActionListener(e -> stepInstruction());
        animateButton.addActionListener(e -> animateInstruction());
        resetButton.addActionListener(e -> resetSimulator()); 
        stepBackButton.addActionListener(e -> stepBack()); 

        pack();
        setLocationRelativeTo(null);
    }
    private void resetSimulator() {
        registers = new long[32];
        pc = 0x10000;
        program = null;
        labelMap.clear();
        currentMicroSteps.clear();
        currentMicroStepIndex = 0;
        stateHistory.clear();
        outputTextArea.setText("");
        codeTextArea.setText("");
        datapathPanel.setActiveComponentsAndBuses(new ArrayList<>(), new ArrayList<>(), new HashMap<>());
        initializeMemoryFile(); // Reset memory file
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        outputTextArea.append("Simulator reset.\n");
    }

    private void stepBack() {
        if (stateHistory.isEmpty()) {
            outputTextArea.append("No previous state to step back to.\n");
            return;
        }

        // Remove current state and restore previous
        stateHistory.remove(stateHistory.size() - 1);
        if (!stateHistory.isEmpty()) {
            StateSnapshot prevState = stateHistory.get(stateHistory.size() - 1);
            pc = prevState.pc;
            registers = prevState.registers.clone();
            currentMicroStepIndex = prevState.microStepIndex;

            // Recompute micro-steps for the current instruction
            if (pc >= 0x10000 && pc < (program.size() * INSTRUCTION_SIZE + 0x10000)) {
                String instruction = program.get((int) ((pc - 0x10000) / INSTRUCTION_SIZE));
                currentMicroSteps = stepAndGetMicroSteps(instruction);
                if (currentMicroStepIndex > 0) {
                    MicroStep step = currentMicroSteps.get(currentMicroStepIndex - 1);
                    outputTextArea.append("Stepped back to: " + step.getOperation() + ": " + step.getComponents() + "\n");
                    outputTextArea.append("State: " + step.getCpuStateSnapshot() + "\n");
                    datapathPanel.setActiveComponentsAndBuses(
                        step.getActiveComponents(),
                        step.getActiveBuses(),
                        step.getBusDataValues()
                    );
                } else {
                    // If at start of instruction, show no active components
                    datapathPanel.setActiveComponentsAndBuses(new ArrayList<>(), new ArrayList<>(), new HashMap<>());
                    outputTextArea.append("Stepped back to before instruction: " + instruction + "\n");
                }
            }
        } else {
            // No states left; reset to initial state
            pc = 0x10000;
            registers = new long[32];
            currentMicroStepIndex = 0;
            currentMicroSteps.clear();
            datapathPanel.setActiveComponentsAndBuses(new ArrayList<>(), new ArrayList<>(), new HashMap<>());
            outputTextArea.append("Stepped back to initial state.\n");
        }
    }


    private void stepInstruction() {
        if (program == null || program.isEmpty()) {
            loadProgram();
        }
        if (pc >= (program.size() * INSTRUCTION_SIZE + 0x10000)) {
            outputTextArea.append("Program finished.\n");
            return;
        }

        String instruction = program.get((int) ((pc - 0x10000) / INSTRUCTION_SIZE));
        currentMicroSteps = stepAndGetMicroSteps(instruction);

        // Save state before executing micro-step
        stateHistory.add(new StateSnapshot(pc, registers.clone(), currentMicroStepIndex));

        if (currentMicroStepIndex < currentMicroSteps.size()) {
            MicroStep step = currentMicroSteps.get(currentMicroStepIndex);
            outputTextArea.append(step.getOperation() + ": " + step.getComponents() + "\n");
            outputTextArea.append("State: " + step.getCpuStateSnapshot() + "\n");
            datapathPanel.setActiveComponentsAndBuses(
                step.getActiveComponents(),
                step.getActiveBuses(),
                step.getBusDataValues()
            );
            currentMicroStepIndex++;
        } else {
            currentMicroStepIndex = 0;
            logInstructionResult(instruction); 
            pc += INSTRUCTION_SIZE;
        }
    }

    private void animateInstruction() {
        if (program == null || program.isEmpty()) {
            loadProgram();
        }
        if (pc >= (program.size() * INSTRUCTION_SIZE + 0x10000)) {
            outputTextArea.append("Program finished.\n");
            return;
        }

        String instruction = program.get((int) ((pc - 0x10000) / INSTRUCTION_SIZE));
        currentMicroSteps = stepAndGetMicroSteps(instruction);
        currentMicroStepIndex = 0;

        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        animationTimer = new Timer(ANIMATION_DELAY, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentMicroStepIndex < currentMicroSteps.size()) {
                    stateHistory.add(new StateSnapshot(pc, registers.clone(), currentMicroStepIndex));
                    MicroStep step = currentMicroSteps.get(currentMicroStepIndex);
                    outputTextArea.append(step.getOperation() + ": " + step.getComponents() + "\n");
                    outputTextArea.append("State: " + step.getCpuStateSnapshot() + "\n");
                    datapathPanel.setActiveComponentsAndBuses(
                        step.getActiveComponents(),
                        step.getActiveBuses(),
                        step.getBusDataValues()
                    );
                    currentMicroStepIndex++;
                } else {
                    animationTimer.stop();
                    currentMicroStepIndex = 0;
                    logInstructionResult(instruction);
                    pc += INSTRUCTION_SIZE; // Increment PC after completing micro-steps
                    datapathPanel.setActiveComponentsAndBuses(new ArrayList<>(), new ArrayList<>(), new HashMap<>());
                }
            }
        });
        animationTimer.start();
    }

    private void loadProgram() {
        String code = codeTextArea.getText().trim();
        if (!code.isEmpty()) {
            program = new ArrayList<>();
            labelMap.clear();
            String[] lines = code.split("\n");
            long currentAddress = 0x10000;
            for (String line : lines) {
                line = line.trim().toUpperCase();
                if (line.isEmpty()) continue;
                if (line.endsWith(":")) {
                    String label = line.substring(0, line.length() - 1);
                    labelMap.put(label, currentAddress);
                } else {
                    program.add(line);
                    currentAddress += INSTRUCTION_SIZE;
                }
            }
            pc = 0x10000;
            currentMicroSteps.clear();
            currentMicroStepIndex = 0;
            outputTextArea.setText("");
            registers = new long[32]; // Reset registers
            initializeMemoryFile(); // Reset memory file on new program
        }
    }

    private String getInstructionEncoding(String instruction) {
        Instruction instr = instructionFactory.createFromAssembly(instruction);
        if (instr != null) {
            // Convert BitSet to hexadecimal string
            java.util.BitSet bytecode = instr.getBytecode();
            StringBuilder hex = new StringBuilder("0x");
            for (int i = 31; i >= 0; i -= 4) {
                int nibble = 0;
                for (int j = 0; j < 4; j++) {
                    if (bytecode.get(i - j)) {
                        nibble |= (1 << j);
                    }
                }
                hex.append(Integer.toHexString(nibble).toUpperCase());
            }
            return hex.toString();
        }
        return "0xUNKNOWN"; // Fallback
    }

    private List<MicroStep> stepAndGetMicroSteps(String instruction) {
        List<MicroStep> microSteps = new ArrayList<>();
        instruction = instruction.trim().toUpperCase();
        String instructionEncoding = getInstructionEncoding(instruction);

        // Regular expressions for instruction parsing
        Pattern addiPattern = Pattern.compile("^ADDI +X(\\d+), +X(\\d+), +#(-?\\d+)$");
        Pattern addPattern = Pattern.compile("^ADD +X(\\d+), +X(\\d+), +X(\\d+)$");
        Pattern cbzPattern = Pattern.compile("^CBZ +X(\\d+), +(\\w+)$");

        Matcher addiMatcher = addiPattern.matcher(instruction);
        Matcher addMatcher = addPattern.matcher(instruction);
        Matcher cbzMatcher = cbzPattern.matcher(instruction);

        if (addiMatcher.matches()) {
            try {
                int rd = Integer.parseInt(addiMatcher.group(1));
                int rn = Integer.parseInt(addiMatcher.group(2));
                int imm = Integer.parseInt(addiMatcher.group(3));
                if (rd > 31 || rn > 31) {
                    outputTextArea.append("Invalid register number in ADDI: " + instruction + "\n");
                    return microSteps;
                }

                // Micro-step 1: Fetch Instruction and PC Increment
                Map<String, String> busData = new HashMap<>();
                busData.put("PC_TO_INSTRUCTION_MEMORY", String.format("0x%X", pc));
                busData.put("INSTRUCTION_MEMORY_TO_INSTRUCTION_REGISTER", instructionEncoding);
                busData.put("ADD_4_TO_ADD_1", "4");
                busData.put("ADD_4_TO_PC", String.format("0x%X", pc + 4));
                microSteps.add(new MicroStep(
                    "Fetch Instruction & PC Increment",
                    "PC, Instruction Memory, Instruction Register, Add 4",
                    "PC=0x" + Long.toHexString(pc) + ", Instruction: " + instruction,
                    List.of("PC", "INSTRUCTION_MEMORY", "INSTRUCTION_REGISTER", "ADD_4"),
                    List.of("PC_TO_INSTRUCTION_MEMORY", "INSTRUCTION_MEMORY_TO_INSTRUCTION_REGISTER",
                            "ADD_4_TO_ADD_1", "ADD_4_TO_PC"),
                    busData
                ));

                // Micro-step 2: Instruction Decode
                busData = new HashMap<>();
                busData.put("INSTRUCTION_MEMORY_TO_CONTROL_UNIT", instructionEncoding);
                busData.put("INSTRUCTION_MEMORY_TO_SIGN_EXTEND", String.valueOf(imm));
                busData.put("CONTROL_ALUSRC_TO_MUX_ALUsrc", "1");
                busData.put("CONTROL_REGWRITE_TO_REGISTERS", "1");
                busData.put("CONTROL_MEMTOREG_TO_MUX_memtoreg", "0");
                microSteps.add(new MicroStep(
                    "Instruction Decode",
                    "Instruction Memory, Control Unit, Sign Extend",
                    "Control Signals: ALUSrc=1, RegWrite=1, MemtoReg=0",
                    List.of("INSTRUCTION_MEMORY", "CONTROL_UNIT", "SIGN_EXTEND"),
                    List.of("INSTRUCTION_MEMORY_TO_CONTROL_UNIT", "INSTRUCTION_MEMORY_TO_SIGN_EXTEND",
                            "CONTROL_ALUSRC_TO_MUX_ALUsrc", "CONTROL_REGWRITE_TO_REGISTERS",
                            "CONTROL_MEMTOREG_TO_MUX_memtoreg"),
                    busData
                ));

                // Micro-step 3: Register Read
                long rnValue = registers[rn];
                busData = new HashMap<>();
                busData.put("REGISTERS_TO_ALU_READ1", String.valueOf(rnValue));
                microSteps.add(new MicroStep(
                    "Register Read",
                    "Registers",
                    "X" + rn + "=" + rnValue,
                    List.of("REGISTERS"),
                    List.of("REGISTERS_TO_ALU_READ1"),
                    busData
                ));

                // Micro-step 4: Sign-Extend Immediate
                busData = new HashMap<>();
                busData.put("SIGN_EXTEND_TO_MUX_ALUsrc", String.valueOf(imm));
                busData.put("MUX_ALUsrc_TO_ALU", String.valueOf(imm));
                microSteps.add(new MicroStep(
                    "Sign-Extend Immediate",
                    "Sign Extend, MUX_ALUsrc",
                    "Immediate: " + imm,
                    List.of("SIGN_EXTEND", "MUX_ALUsrc"),
                    List.of("SIGN_EXTEND_TO_MUX_ALUsrc", "MUX_ALUsrc_TO_ALU"),
                    busData
                ));

                // Micro-step 5: ALU Compute
                long result = rnValue + imm;
                busData = new HashMap<>();
                busData.put("REGISTERS_TO_ALU_READ1", String.valueOf(rnValue));
                busData.put("MUX_ALUsrc_TO_ALU", String.valueOf(imm));
                busData.put("ALU_TO_MUX_memtoreg_RESULT", String.valueOf(result));
                microSteps.add(new MicroStep(
                    "ALU Compute",
                    "ALU",
                    "Result: " + result,
                    List.of("ALU"),
                    List.of("REGISTERS_TO_ALU_READ1", "MUX_ALUsrc_TO_ALU", "ALU_TO_MUX_memtoreg_RESULT"),
                    busData
                ));

                // Micro-step 6: MUX_memtoreg Select
                busData = new HashMap<>();
                busData.put("ALU_TO_MUX_memtoreg_RESULT", String.valueOf(result));
                busData.put("MUX_memtoreg_TO_REGISTERS_WRITE", String.valueOf(result));
                microSteps.add(new MicroStep(
                    "Select Write Data",
                    "MUX_memtoreg",
                    "Write Data: " + result,
                    List.of("MUX_memtoreg"),
                    List.of("ALU_TO_MUX_memtoreg_RESULT", "MUX_memtoreg_TO_REGISTERS_WRITE"),
                    busData
                ));

                // Micro-step 7: Register Write
                registers[rd] = result;
                busData = new HashMap<>();
                busData.put("MUX_memtoreg_TO_REGISTERS_WRITE", String.valueOf(result));
                busData.put("CONTROL_REGWRITE_TO_REGISTERS", "1");
                microSteps.add(new MicroStep(
                    "Register Write",
                    "Registers",
                    "X" + rd + "=" + result,
                    List.of("REGISTERS"),
                    List.of("MUX_memtoreg_TO_REGISTERS_WRITE", "CONTROL_REGWRITE_TO_REGISTERS"),
                    busData
                ));

            } catch (NumberFormatException e) {
                outputTextArea.append("Invalid ADDI format: " + instruction + "\n");
            }
        } else if (addMatcher.matches()) {
            try {
                int rd = Integer.parseInt(addMatcher.group(1));
                int rn = Integer.parseInt(addMatcher.group(2));
                int rm = Integer.parseInt(addMatcher.group(3));
                if (rd > 31 || rn > 31 || rm > 31) {
                    outputTextArea.append("Invalid register number in ADD: " + instruction + "\n");
                    return microSteps;
                }

                // Micro-step 1: Fetch Instruction and PC Increment
                Map<String, String> busData = new HashMap<>();
                busData.put("PC_TO_INSTRUCTION_MEMORY", String.format("0x%X", pc));
                busData.put("INSTRUCTION_MEMORY_TO_INSTRUCTION_REGISTER", instructionEncoding);
                busData.put("ADD_4_TO_ADD_1", "4");
                busData.put("ADD_4_TO_PC", String.format("0x%X", pc + 4));
                microSteps.add(new MicroStep(
                    "Fetch Instruction & PC Increment",
                    "PC, Instruction Memory, Instruction Register, Add 4",
                    "PC=0x" + Long.toHexString(pc) + ", Instruction: " + instruction,
                    List.of("PC", "INSTRUCTION_MEMORY", "INSTRUCTION_REGISTER", "ADD_4"),
                    List.of("PC_TO_INSTRUCTION_MEMORY", "INSTRUCTION_MEMORY_TO_INSTRUCTION_REGISTER",
                            "ADD_4_TO_ADD_1", "ADD_4_TO_PC"),
                    busData
                ));

                // Micro-step 2: Instruction Decode
                busData = new HashMap<>();
                busData.put("INSTRUCTION_MEMORY_TO_CONTROL_UNIT", instructionEncoding);
                busData.put("INSTRUCTION_MEMORY_TO_REGISTERS_READ1", "X" + rn);
                busData.put("INSTRUCTION_MEMORY_TO_MUX_reg2loc_0", "X" + rm);
                busData.put("INSTRUCTION_MEMORY_TO_MUX_reg2loc_1", "X" + rm);
                busData.put("MUX_reg2loc_TO_REGISTERS_READ2", "X" + rm);
                busData.put("CONTROL_ALUSRC_TO_MUX_ALUsrc", "0");
                busData.put("CONTROL_REGWRITE_TO_REGISTERS", "1");
                busData.put("CONTROL_MEMTOREG_TO_MUX_memtoreg", "0");
                busData.put("CONTROL_REG2LOC_TO_MUX_reg2loc", "1");
                microSteps.add(new MicroStep(
                    "Instruction Decode",
                    "Instruction Memory, Control Unit, Registers, MUX_reg2loc",
                    "Control Signals: ALUSrc=0, RegWrite=1, MemtoReg=0, Reg2Loc=1",
                    List.of("INSTRUCTION_MEMORY", "CONTROL_UNIT", "REGISTERS", "MUX_reg2loc"),
                    List.of("INSTRUCTION_MEMORY_TO_CONTROL_UNIT", "INSTRUCTION_MEMORY_TO_REGISTERS_READ1",
                            "INSTRUCTION_MEMORY_TO_MUX_reg2loc_0", "INSTRUCTION_MEMORY_TO_MUX_reg2loc_1",
                            "MUX_reg2loc_TO_REGISTERS_READ2", "CONTROL_ALUSRC_TO_MUX_ALUsrc",
                            "CONTROL_REGWRITE_TO_REGISTERS", "CONTROL_MEMTOREG_TO_MUX_memtoreg",
                            "CONTROL_REG2LOC_TO_MUX_reg2loc"),
                    busData
                ));

                // Micro-step 3: Register Read
                long rnValue = registers[rn];
                long rmValue = registers[rm];
                busData = new HashMap<>();
                busData.put("REGISTERS_TO_ALU_READ1", String.valueOf(rnValue));
                busData.put("REGISTERS_TO_MUX_ALUsrc_READ2", String.valueOf(rmValue));
                busData.put("MUX_ALUsrc_TO_ALU", String.valueOf(rmValue));
                microSteps.add(new MicroStep(
                    "Register Read",
                    "Registers, MUX_ALUsrc",
                    "X" + rn + "=" + rnValue + ", X" + rm + "=" + rmValue,
                    List.of("REGISTERS", "MUX_ALUsrc"),
                    List.of("REGISTERS_TO_ALU_READ1", "REGISTERS_TO_MUX_ALUsrc_READ2", "MUX_ALUsrc_TO_ALU"),
                    busData
                ));

                // Micro-step 4: ALU Compute
                long result = rnValue + rmValue;
                busData = new HashMap<>();
                busData.put("REGISTERS_TO_ALU_READ1", String.valueOf(rnValue));
                busData.put("MUX_ALUsrc_TO_ALU", String.valueOf(rmValue));
                busData.put("ALU_TO_MUX_memtoreg_RESULT", String.valueOf(result));
                microSteps.add(new MicroStep(
                    "ALU Compute",
                    "ALU",
                    "Result: " + result,
                    List.of("ALU"),
                    List.of("REGISTERS_TO_ALU_READ1", "MUX_ALUsrc_TO_ALU", "ALU_TO_MUX_memtoreg_RESULT"),
                    busData
                ));

                // Micro-step 5: MUX_memtoreg Select
                busData = new HashMap<>();
                busData.put("ALU_TO_MUX_memtoreg_RESULT", String.valueOf(result));
                busData.put("MUX_memtoreg_TO_REGISTERS_WRITE", String.valueOf(result));
                microSteps.add(new MicroStep(
                    "Select Write Data",
                    "MUX_memtoreg",
                    "Write Data: " + result,
                    List.of("MUX_memtoreg"),
                    List.of("ALU_TO_MUX_memtoreg_RESULT", "MUX_memtoreg_TO_REGISTERS_WRITE"),
                    busData
                ));

                // Micro-step 6: Register Write
                registers[rd] = result;
                busData = new HashMap<>();
                busData.put("MUX_memtoreg_TO_REGISTERS_WRITE", String.valueOf(result));
                busData.put("CONTROL_REGWRITE_TO_REGISTERS", "1");
                microSteps.add(new MicroStep(
                    "Register Write",
                    "Registers",
                    "X" + rd + "=" + result,
                    List.of("REGISTERS"),
                    List.of("MUX_memtoreg_TO_REGISTERS_WRITE", "CONTROL_REGWRITE_TO_REGISTERS"),
                    busData
                ));

            } catch (NumberFormatException e) {
                outputTextArea.append("Invalid ADD format: " + instruction + "\n");
            }
        } else if (cbzMatcher.matches()) {
            try {
                int rt = Integer.parseInt(cbzMatcher.group(1));
                String label = cbzMatcher.group(2);
                if (rt > 31) {
                    outputTextArea.append("Invalid register number in CBZ: " + instruction + "\n");
                    return microSteps;
                }
                if (!labelMap.containsKey(label)) {
                    outputTextArea.append("Unknown label in CBZ: " + label + "\n");
                    return microSteps;
                }
                long targetAddress = labelMap.get(label);
                long offset = targetAddress - pc; // Byte offset

                // Micro-step 1: Fetch Instruction and PC Increment
                Map<String, String> busData = new HashMap<>();
                busData.put("PC_TO_INSTRUCTION_MEMORY", String.format("0x%X", pc));
                busData.put("INSTRUCTION_MEMORY_TO_INSTRUCTION_REGISTER", instructionEncoding);
                busData.put("ADD_4_TO_ADD_1", "4");
                busData.put("ADD_4_TO_PC", String.format("0x%X", pc + 4));
                microSteps.add(new MicroStep(
                    "Fetch Instruction & PC Increment",
                    "PC, Instruction Memory, Instruction Register, Add 4",
                    "PC=0x" + Long.toHexString(pc) + ", Instruction: " + instruction,
                    List.of("PC", "INSTRUCTION_MEMORY", "INSTRUCTION_REGISTER", "ADD_4"),
                    List.of("PC_TO_INSTRUCTION_MEMORY", "INSTRUCTION_MEMORY_TO_INSTRUCTION_REGISTER",
                            "ADD_4_TO_ADD_1", "ADD_4_TO_PC"),
                    busData
                ));

                // Micro-step 2: Instruction Decode
                busData = new HashMap<>();
                busData.put("INSTRUCTION_MEMORY_TO_CONTROL_UNIT", instructionEncoding);
                busData.put("INSTRUCTION_MEMORY_TO_REGISTERS_READ1", "X" + rt);
                busData.put("INSTRUCTION_MEMORY_TO_SIGN_EXTEND", String.valueOf(offset / 4));
                busData.put("SIGN_EXTEND_TO_SHIFT_LEFT_2", String.valueOf(offset / 4));
                busData.put("SHIFT_LEFT_2_TO_ADD", String.valueOf(offset));
                busData.put("ADD_2_TO_MUX_PCSRC", String.format("0x%X", pc + offset));
                busData.put("CONTROL_BRANCH_TO_AND_GATE", "1");
                microSteps.add(new MicroStep(
                    "Instruction Decode",
                    "Instruction Memory, Control Unit, Registers, Sign Extend, Shift Left 2, Add 2",
                    "Control Signals: Branch=1",
                    List.of("INSTRUCTION_MEMORY", "CONTROL_UNIT", "REGISTERS", "SIGN_EXTEND", "SHIFT_LEFT_2", "ADD_2"),
                    List.of("INSTRUCTION_MEMORY_TO_CONTROL_UNIT", "INSTRUCTION_MEMORY_TO_REGISTERS_READ1",
                            "INSTRUCTION_MEMORY_TO_SIGN_EXTEND", "SIGN_EXTEND_TO_SHIFT_LEFT_2",
                            "SHIFT_LEFT_2_TO_ADD", "ADD_2_TO_MUX_PCSRC", "CONTROL_BRANCH_TO_AND_GATE"),
                    busData
                ));

                // Micro-step 3: Branch Decision
                long rtValue = registers[rt];
                boolean isZero = rtValue == 0;
                String condition = isZero ? "Branch taken" : "Branch not taken";
                busData = new HashMap<>();
                busData.put("REGISTERS_TO_ALU_READ1", String.valueOf(rtValue));
                busData.put("ALU_TO_AND_GATE", String.valueOf(isZero ? 1 : 0));
                busData.put("AND_GATE_TO_OR_GATE", String.valueOf(isZero ? 1 : 0));
                busData.put("OR_GATE_TO_MUX_PCSRC", String.valueOf(isZero ? 1 : 0));
                microSteps.add(new MicroStep(
                    "Branch Decision",
                    "ALU, AND Gate, OR Gate, MUX_PCSrc",
                    "X" + rt + "=" + rtValue + ", " + condition,
                    List.of("ALU", "AND_GATE", "OR_GATE", "MUX_PCSRC"),
                    List.of("REGISTERS_TO_ALU_READ1", "ALU_TO_AND_GATE", "AND_GATE_TO_OR_GATE", "OR_GATE_TO_MUX_PCSRC"),
                    busData
                ));

                // Micro-step 4: Update PC
                busData = new HashMap<>();
                if (isZero) {
                    pc = targetAddress;
                    busData.put("ADD_2_TO_MUX_PCSRC", String.format("0x%X", pc));
                    busData.put("MUX_PCSRC_TO_PC", String.format("0x%X", pc));
                    microSteps.add(new MicroStep(
                        "Update PC (Branch Taken)",
                        "MUX_PCSrc, PC",
                        "PC=0x" + Long.toHexString(pc),
                        List.of("MUX_PCSRC", "PC"),
                        List.of("ADD_2_TO_MUX_PCSRC", "MUX_PCSRC_TO_PC"),
                        busData
                    ));
                } else {
                    busData.put("ADD_1_TO_MUX_PCSRC", String.format("0x%X", pc + 4));
                    busData.put("MUX_PCSRC_TO_PC", String.format("0x%X", pc + 4));
                    microSteps.add(new MicroStep(
                        "Update PC (Branch Not Taken)",
                        "MUX_PCSrc, PC",
                        "PC=0x" + Long.toHexString(pc + 4),
                        List.of("MUX_PCSRC", "PC"),
                        List.of("ADD_1_TO_MUX_PCSRC", "MUX_PCSRC_TO_PC"),
                        busData
                    ));
                }

            } catch (NumberFormatException e) {
                outputTextArea.append("Invalid CBZ format: " + instruction + "\n");
            }
        } else {
            outputTextArea.append("Unsupported instruction: " + instruction + "\n");
        }

        return microSteps;
    }


    private class StateSnapshot {
        private final long pc;
        private final long[] registers;
        private final int microStepIndex;

        public StateSnapshot(long pc, long[] registers, int microStepIndex) {
            this.pc = pc;
            this.registers = registers;
            this.microStepIndex = microStepIndex;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new CPUSimulatorWithDatapathGUI().setVisible(true);
        });
    }
}