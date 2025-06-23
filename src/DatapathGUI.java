import instruction.*;
import core.*;
import datapath.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Map;

import java.util.LinkedList;
import java.util.Queue;

/**
 * The main graphical user interface for the LEGv8 Simulator.
 * This version includes a detailed micro-step log.
 */
public class DatapathGUI extends JFrame {

    private final CPUSimulator simulator;

    private final DatapathPanel datapathPanel;
    private JTextArea codeTextArea;
    private JTextArea outputTextArea;
    private JButton loadButton, stepButton, runButton, resetButton;
    private Timer runTimer;

    private final Queue<String> logQueue;

    public DatapathGUI() {
        super("LEGv8 Datapath Simulator");
        this.logQueue = new LinkedList<>();

        InstructionConfigLoader configLoader = new InstructionConfigLoader();
        if (!configLoader.loadConfig("src/instruction/instructions.txt")) {
            JOptionPane.showMessageDialog(this, "Could not load instructions.txt", "Error", JOptionPane.ERROR_MESSAGE);
        }
        this.simulator = new CPUSimulator(configLoader);

        this.datapathPanel = new DatapathPanel();
        initGUI();
        log("Simulator ready. Please load a program.");
        updateGUI(); 
    }

    private void initGUI() {
        setTitle("LEGv8 Datapath Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Center: Datapath
        JScrollPane datapathScroll = new JScrollPane(datapathPanel);
        add(datapathScroll, BorderLayout.CENTER);

        // Right: Side Panel
        JPanel sidePanel = new JPanel(new BorderLayout(10, 10));
        sidePanel.setPreferredSize(new Dimension(400, 700)); // Increased width for better logging
        add(sidePanel, BorderLayout.EAST);

        // Top-Right: Controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controls"));
        loadButton = new JButton("Load Program");
        stepButton = new JButton("Step");
        runButton = new JButton("Run");
        resetButton = new JButton("Reset");
        controlPanel.add(loadButton);
        controlPanel.add(stepButton);
        controlPanel.add(runButton);
        controlPanel.add(resetButton);
        sidePanel.add(controlPanel, BorderLayout.NORTH);

        // Middle-Right: Code
        JPanel codePanel = new JPanel(new BorderLayout(5, 5));
        codePanel.setBorder(BorderFactory.createTitledBorder("LEGv8 Assembly Code"));
        codeTextArea = new JTextArea(20, 30);
        codeTextArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        codeTextArea.setText(getSampleProgram());
        codePanel.add(new JScrollPane(codeTextArea), BorderLayout.CENTER);
        sidePanel.add(codePanel, BorderLayout.CENTER);
        
        // Bottom-Right: Log/State
        outputTextArea = new JTextArea(15, 30);
        outputTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputTextArea.setEditable(false);
        JScrollPane outputScroll = new JScrollPane(outputTextArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Micro-Step Log & CPU State"));
        sidePanel.add(outputScroll, BorderLayout.SOUTH);

        // Action Listeners
        loadButton.addActionListener(this::handleLoad);
        stepButton.addActionListener(this::handleStep);
        runButton.addActionListener(this::handleRun);
        resetButton.addActionListener(this::handleReset);

        runTimer = new Timer(5000, e -> handleStep(null)); 

        setSize(1700, 950); 
        setLocationRelativeTo(null);
    }

    /**
     * Handles the Load button action.
     * Loads the program from the codeTextArea into the simulator.
     * Clears the outputTextArea and updates the GUI.
     */
    private void handleLoad(ActionEvent e) {
        if (runTimer.isRunning()) runTimer.stop();
        
        String[] programLines = codeTextArea.getText().split("\\n");
        try {
            simulator.loadProgram(programLines);
            outputTextArea.setText(""); 
            log("Program loaded successfully with " + simulator.getInstructionCount() + " instructions.");
        } catch (Exception ex) {
            log("ERROR loading program: " + ex.getMessage());
        }
        updateGUI();
    }

    private void handleStep(ActionEvent e) {
        if (simulator.isFinished()) {
            if (runTimer.isRunning()) runTimer.stop();
            log("--- Execution finished ---");
            updateGUI(true); 
            return;
        }

        simulator.step();

        updateGUI(false);
    }

    private void handleRun(ActionEvent e) {
        if (runTimer.isRunning()) {
            runTimer.stop();
        } else {
            if (simulator.isFinished()) {
                log("Program already finished. Please Load or Reset.");
                return;
            }
            log("--- Running program automatically ---");
            runTimer.start();
        }
        updateGUI(); 
    }

    private void handleReset(ActionEvent e) {
        if (runTimer.isRunning()) runTimer.stop();
        simulator.reset();
        codeTextArea.setText(getSampleProgram());
        outputTextArea.setText(""); 
        log("--- Simulator Reset ---");
        updateGUI();
    }
    
    
    private void updateGUI(boolean isFinalUpdate) {
        datapathPanel.setActiveComponentsAndBuses(
            simulator.getActiveComponents(),
            simulator.getActiveBuses(),
            simulator.getBusDataValues()
        );

        log(String.format("PC: %d | Instruction: %s", simulator.getPc(), simulator.getLastExecutedInstruction()));
        log("Current Micro-Step: " + simulator.getCurrentMicroStepDescription());
        
        StringBuilder currentLog = new StringBuilder(outputTextArea.getText());
        while (!logQueue.isEmpty()) {
            currentLog.append(logQueue.poll()).append("\n");
        }
        outputTextArea.setText(currentLog.toString());
        
        outputTextArea.append(generateStateDisplay());
        outputTextArea.setCaretPosition(outputTextArea.getDocument().getLength());
        
        boolean finished = simulator.isFinished();
        stepButton.setEnabled(!finished && !runTimer.isRunning());
        loadButton.setEnabled(!runTimer.isRunning());
        runButton.setText(runTimer.isRunning() ? "Pause" : "Run");

        if (isFinalUpdate || simulator.getCurrentMicroStepDescription().equals("Ready for next instruction.")) {
             outputTextArea.setText("");
        }
    }
    
    private void log(String message) {
        outputTextArea.append(message + "\n");
    }

    private void updateGUI() {
        datapathPanel.setActiveComponentsAndBuses(
            simulator.getActiveComponents(),
            simulator.getActiveBuses(),
            simulator.getBusDataValues()
        );

        StringBuilder currentLog = new StringBuilder(outputTextArea.getText());
        while (!logQueue.isEmpty()) {
            currentLog.append(logQueue.poll()).append("\n");
        }
        outputTextArea.setText(currentLog.toString());
        
        outputTextArea.append(generateStateDisplay());
        outputTextArea.setCaretPosition(outputTextArea.getDocument().getLength()); // Auto-scroll to the bottom

        boolean finished = simulator.isFinished();
        stepButton.setEnabled(!finished && !runTimer.isRunning());
        loadButton.setEnabled(!runTimer.isRunning());
        runButton.setText(runTimer.isRunning() ? "Pause" : "Run");
    }

    /**
     * Creates a formatted string of the current CPU state (registers and memory).
     * @return A string representing the CPU state.
     */
    private String generateStateDisplay() {
        StringBuilder state = new StringBuilder();
        state.append("\n\n--- CPU STATE ---\n");
        for (int i = 0; i < 32; i++) {
            long val = simulator.getRegisterValue(i);
            if (val != 0 || i == 0) { // Always show X0
                state.append(String.format("X%-2d: %-12d (0x%X)\n", i, val, val));
            }
        }
        state.append("\n--- MEMORY (Non-Zero) ---\n");
        Map<Long, Long> mem = simulator.getMemoryState();
        if (mem.isEmpty()) {
            state.append("All memory is zero.\n");
        } else {
            mem.forEach((addr, val) ->
                state.append(String.format("Mem[0x%X]: %-12d (0x%X)\n", addr, val, val))
            );
        }
        return state.toString();
    }

    // /**
    //  * Adds a message to the log queue, to be displayed on the next GUI update.
    //  * @param message The string message to log.
    //  */    
    private String getSampleProgram() {
        return "ADDI X1, X0, #100\n" +
               "MOVZ X2, #5, LSL #0\n" +
               "STUR X2, [X1, #16]\n" +
               "LDUR X3, [X1, #16]\n" +
               "ADD X4, X3, X1\n" +
               "SUB X5, X4, X2\n";
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new DatapathGUI().setVisible(true);
        });
    }
}
