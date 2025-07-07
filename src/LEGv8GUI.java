import core.*;
import datapath.DatapathPanel;
import datapath.MicroStep;
import datapath.PipelineStage;
import instruction.Instruction;
import instruction.InstructionConfigLoader;
import memory.*;


import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;



public class LEGv8GUI {
private DatapathPanel datapathPanel;
private JFrame datapathFrame;
    private final CPUSimulator simulator;
    private JFrame frame;
    private JTextArea codeEditor;
    private JTable memoryTable;
    private JTable registersTable;
    private JTable instructionTable;
    private JTextArea outputArea;
 //   private JLabel pcLabel;
    private JComboBox<String> memoryTabSelector;

    private JLabel zeroFlagLabel;
    private JLabel negativeFlagLabel;
    private JLabel overflowFlagLabel;
    private JLabel carryFlagLabel;

    // Định nghĩa màu sắc
    private final Color BACKGROUND_COLOR = new Color(75, 22, 76); // Purple rgb(75, 22, 76)
    private final Color CODE_EDITOR_BG = new Color(248, 231, 246); // rgb(248, 231, 246)
    private final Color CODE_EDITOR_FG = Color.BLACK;
    private final Color TABLE_CELL_BG = new Color(248, 231, 246); // rgb(248, 231, 246)
    private final Color TABLE_HEADER_BG = new Color(186, 107, 173); // rgb(186, 107, 173)
    private final Color TABLE_FG = Color.BLACK;
    private final Color BUTTON_BG = new Color(221, 136, 207); // rgb(221, 136, 207)
    private final Color BUTTON_FG = Color.WHITE;
    private final Color BUTTON_HOVER_BG = new Color(100, 149, 237); // rgb(100, 149, 237)
    private final Color TITLE_FG = new Color(245, 245, 245); // rgb(245, 245, 245)
    private final Color TITLE_AUTHOR = new Color(125, 89, 120);  // rgb(125, 89, 120)

    public LEGv8GUI(CPUSimulator simulator) {
        this.simulator = simulator;
        initializeGUI();
    }

    private void initializeGUI() {
        // Tạo frame chính
        frame = new JFrame("LEGv8 Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1650, 650);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(BACKGROUND_COLOR);

        // Panel chứa tiêu đề và nút trên cùng một dòng
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(BACKGROUND_COLOR);

        // Panel trên cùng (chứa tiêu đề và Code Editor)
        final int PADDING = 5;
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 50, 0));
        headerPanel.setBackground(BACKGROUND_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // Tiêu đề "LEGv8 Simulator" ở góc trên bên trái
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(BACKGROUND_COLOR);
        JLabel titleLabel = new JLabel("LEGv8 Simulator");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 40));
        titleLabel.setForeground(TITLE_FG);
        titlePanel.add(titleLabel);

        // Panel chứa các nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BACKGROUND_COLOR);
        JButton assembleButton = createStyledButton("Assemble");
        JButton runButton = createStyledButton("Run");
        JButton restartButton = createStyledButton("Restart");
        JButton clearAllButton = createStyledButton("Clear All");
        JButton datapathButton = createStyledButton("Datapath");
        JButton helpButton = createStyledButton("Help");
        buttonPanel.add(assembleButton);
        buttonPanel.add(runButton);
        buttonPanel.add(restartButton);
        buttonPanel.add(clearAllButton);
        buttonPanel.add(helpButton);
        buttonPanel.add(datapathButton);

        // Thêm titlePanel và buttonPanel vào headerPanel
        headerPanel.add(titlePanel);
        headerPanel.add(buttonPanel);
        topPanel.add(headerPanel, BorderLayout.NORTH);

        // Panel chứa Code Editor, Instruction, và Status
        final int PADDING1 = 10;
        final int LEFT_PADDING1 = 20;
        final int RIGHT_PADDING1 = 20;
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(BACKGROUND_COLOR);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(PADDING1, LEFT_PADDING1, PADDING1, RIGHT_PADDING1));

        // Panel bên trái chứa Code Editor và Instruction
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(BACKGROUND_COLOR);

        // Panel Code Editor
        JPanel codePanel = new JPanel(new BorderLayout());
        codePanel.setBackground(BACKGROUND_COLOR);
        
        codeEditor = new JTextArea(15, 100);
        codeEditor.setFont(new Font("Monospaced", Font.PLAIN, 12));
        codeEditor.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
        codeEditor.setBackground(CODE_EDITOR_BG);
        codeEditor.setForeground(CODE_EDITOR_FG);
        JScrollPane codeScroll = new JScrollPane(codeEditor);

        // Tạo panel tùy chỉnh cho tiêu đề "Code Editor" 
        class TabPanel extends JPanel {
            public TabPanel() {
                setOpaque(false); // Nền trong suốt
                setPreferredSize(new Dimension(150, 25)); // Kích thước của tab
                setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5)); // Đặt vị trí chữ

                JLabel titleLabelEdit = new JLabel("Code Editor");
                titleLabelEdit.setForeground(BACKGROUND_COLOR);
                titleLabelEdit.setFont(new Font("Arial", Font.BOLD, 15));
                add(titleLabelEdit);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Vẽ hình dạng tab với góc nghiêng
                int width = getWidth();
                int height = getHeight();
                Polygon tabShape = new Polygon();
                tabShape.addPoint(0, height);        // Điểm dưới bên trái
                tabShape.addPoint(0, 0);             // Điểm trên bên trái
                tabShape.addPoint(width - 20, 0);    // Điểm trên bên phải (trước góc nghiêng)
                tabShape.addPoint(width, height);    // Điểm dưới bên phải (sau góc nghiêng)

                // Vẽ nền trắng cho tab
                g2d.setColor(Color.WHITE);
                g2d.fillPolygon(tabShape);

                // Vẽ viền đen
                // g2d.setColor(Color.BLACK);
                // g2d.setStroke(new BasicStroke(1));
                // g2d.drawPolygon(tabShape);

                g2d.dispose();
            }
        }

        // Tạo panel chứa tiêu đề và phần nền phía sau
        JPanel headerCodePanel = new JPanel(new BorderLayout());
        headerCodePanel.setBackground(BACKGROUND_COLOR);
        headerCodePanel.add(new TabPanel(), BorderLayout.WEST);

        // Thêm khoảng cách giữa tiêu đề và code editor
        headerCodePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        codePanel.add(headerCodePanel, BorderLayout.NORTH);
        codePanel.add(codeScroll, BorderLayout.SOUTH);
        leftPanel.add(codePanel, BorderLayout.NORTH);

        // Panel Instruction
        JPanel instructionPanel = new JPanel(new BorderLayout());
        instructionPanel.setBackground(BACKGROUND_COLOR);
        instructionTable = new JTable(
                new DefaultTableModel(new Object[] { "Line", "Address", "Source", "Meaning" }, 0));
        styleInstructionTable(instructionTable);
        populateInstructionTable();
        JScrollPane instructionScroll = new JScrollPane(instructionTable);
        instructionScroll.setPreferredSize(new Dimension(0, 150)); // Chiều cao 150px
        JLabel titleinstruc = new JLabel("Instruction");
        titleinstruc.setForeground(BACKGROUND_COLOR);
        instructionPanel.add(titleinstruc, BorderLayout.NORTH);
        instructionPanel.add(instructionScroll, BorderLayout.CENTER);
        leftPanel.add(instructionPanel, BorderLayout.CENTER);

        centerPanel.add(leftPanel, BorderLayout.WEST);

        // Panel Status (Register và Memory song song)
        JPanel statusWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT)); 

        statusWrapper.setBackground(BACKGROUND_COLOR);
        statusWrapper.setPreferredSize(new Dimension(600, 300)); // Giới hạn chiều cao tối đa 300px
        statusWrapper.setMaximumSize(new Dimension(700, 700));

        JPanel statusPanel = new JPanel(new GridLayout(1, 2));
        statusPanel.setBackground(BACKGROUND_COLOR);

        // Registers Table
        registersTable = new JTable(new DefaultTableModel(new Object[] { "Register", "Value" }, 0));
        styleTable(registersTable);
        populateRegistersTable();
        JScrollPane registersScroll = new JScrollPane(registersTable);
        registersScroll.setPreferredSize(new Dimension(250, 500)); // Chiều cao 200px
        registersScroll.setMaximumSize(new Dimension(250, 700)); // Giới hạn chiều cao tối đa
       

        // Memory Table with Tab Selector
        JPanel memoryPanel = new JPanel(new BorderLayout());
        memoryPanel.setBackground(BACKGROUND_COLOR);
        String[] memoryTabs = {/*  "Data",*/ "Memory" /* , "Text"*/ };
        memoryTabSelector = new JComboBox<>(memoryTabs);
        memoryTabSelector.setSelectedIndex(0);
        memoryTabSelector.setPreferredSize(new Dimension(255, 30));
        memoryTable = new JTable(new DefaultTableModel(new Object[] { "Address", "Value" }, 0));
        styleTable(memoryTable);
        populateMemoryTable();
        JScrollPane memoryScroll = new JScrollPane(memoryTable);
        memoryScroll.setPreferredSize(new Dimension(255, 170)); // Chiều cao 170px (300 - 30 cho JComboBox)
        memoryScroll.setMaximumSize(new Dimension(255, 170)); // Giới hạn chiều cao tối đa
        memoryPanel.add(memoryTabSelector, BorderLayout.NORTH);
        memoryPanel.add(memoryScroll, BorderLayout.CENTER);

        // Panel hiển thị cờ trạng thái 
       // JPanel flagsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel flagsPanel = new JPanel(new GridLayout(1, 4, 5, 0));
        flagsPanel.setBackground(BACKGROUND_COLOR);

        // Khung cho Zero Flag
        JPanel zeroFlagPanel = new JPanel();
        zeroFlagPanel.setBackground(BACKGROUND_COLOR);
        zeroFlagPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE), "ZF",
                TitledBorder.CENTER, TitledBorder.TOP, null, Color.WHITE));
        zeroFlagPanel.setPreferredSize(new Dimension(60, 40));
        zeroFlagPanel.setBorder(BorderFactory.createCompoundBorder(
                zeroFlagPanel.getBorder(),
                BorderFactory.createEmptyBorder(-6, 5, 2, 5)));
        zeroFlagLabel = new JLabel("0");
        zeroFlagLabel.setForeground(Color.WHITE);
        zeroFlagPanel.add(zeroFlagLabel);
        flagsPanel.add(zeroFlagPanel);

        // Khung cho Negative Flag
        JPanel negativeFlagPanel = new JPanel();
        negativeFlagPanel.setBackground(BACKGROUND_COLOR);
        negativeFlagPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE), "NF",
                TitledBorder.CENTER, TitledBorder.TOP, null, Color.WHITE));
        negativeFlagPanel.setPreferredSize(new Dimension(60, 40));
        negativeFlagPanel.setBorder(BorderFactory.createCompoundBorder(
                negativeFlagPanel.getBorder(),
                BorderFactory.createEmptyBorder(-6, 5, 2, 5)));
        negativeFlagLabel = new JLabel("0");
        negativeFlagLabel.setForeground(Color.WHITE);
        negativeFlagPanel.add(negativeFlagLabel);
        flagsPanel.add(negativeFlagPanel);

        // Khung cho Overflow Flag
        JPanel overflowFlagPanel = new JPanel();
        overflowFlagPanel.setBackground(BACKGROUND_COLOR);
        overflowFlagPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE), "OF",
                TitledBorder.CENTER, TitledBorder.TOP, null, Color.WHITE));
        overflowFlagPanel.setPreferredSize(new Dimension(60, 40));
        overflowFlagPanel.setBorder(BorderFactory.createCompoundBorder(
                overflowFlagPanel.getBorder(),
                BorderFactory.createEmptyBorder(-6, 5, 2, 5))); 
        overflowFlagLabel = new JLabel("0");
        overflowFlagLabel.setForeground(Color.WHITE);
        overflowFlagPanel.add(overflowFlagLabel);
        flagsPanel.add(overflowFlagPanel);

        // Khung cho Carry Flag
        JPanel carryFlagPanel = new JPanel();
        carryFlagPanel.setBackground(BACKGROUND_COLOR);
        carryFlagPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE), "CF",
                TitledBorder.CENTER, TitledBorder.TOP, null, Color.WHITE));
        carryFlagPanel.setPreferredSize(new Dimension(60, 40)); 
        carryFlagPanel.setBorder(BorderFactory.createCompoundBorder(
                carryFlagPanel.getBorder(),
                BorderFactory.createEmptyBorder(-6, 5, 2, 5))); 
        carryFlagLabel = new JLabel("0");
        carryFlagLabel.setForeground(Color.WHITE);
        carryFlagPanel.add(carryFlagLabel);
        flagsPanel.add(carryFlagPanel);
        memoryPanel.add(flagsPanel, BorderLayout.SOUTH);
        

        // Thêm thông tin tác giả nhiều dòng vào góc dưới bên phải
        JPanel authorPanel = new JPanel(new BorderLayout());
        authorPanel.setBackground(BACKGROUND_COLOR);
        authorPanel.setBorder(BorderFactory.createEmptyBorder(7, 100, 0, -400)); // Padding: 5px trên/dưới, 10px phải
        JTextArea authorTextArea = new JTextArea(3, 100); // 3 dòng, độ rộng 15 ký tự
        authorTextArea.setText("Author:\n 1. Nguyễn Ngọc Duy Mỹ_MSSV: 23120145_Mail:23120145@student.hcmus.edu.vn \n 2. Trịnh Thị Thu Hiền_MSSV: 23120254_Mail: 23120254@student.hcmus.edu.vn");
        authorTextArea.setForeground(TITLE_AUTHOR); 
        authorTextArea.setFont(new Font("Arial", Font.PLAIN, 12)); // Font Arial, size 12
        authorTextArea.setEditable(false); 
        authorTextArea.setOpaque(false);
        authorTextArea.setLineWrap(true); 
        authorPanel.add(authorTextArea, BorderLayout.EAST); 
        
        statusPanel.add(registersScroll);
        statusPanel.add(memoryPanel);
        statusWrapper.add(statusPanel);
        statusWrapper.add(authorPanel);

        centerPanel.add(statusWrapper, BorderLayout.EAST);

        // Thêm các panel vào frame
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);

        // Xử lý sự kiện nút
        assembleButton.addActionListener(e -> assembleProgram());
        runButton.addActionListener(e -> runProgram());
        restartButton.addActionListener(e -> restartProgram());
        clearAllButton.addActionListener(e -> clearAll());
        helpButton.addActionListener(e -> showHelp());
        
        datapathButton.addActionListener(e -> {
    datapathFrame = new JFrame("LEGv8 Datapath Visualization");
    datapathFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    datapathFrame.setSize(1800, 750);

    datapathPanel = new DatapathPanel();

    // Create control panel with Step, Back, Auto Run, and Restart buttons
    JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    JButton stepDatapathButton = new JButton("Step Forward");
    JButton backDatapathButton = new JButton("Step Backward");
    JButton restartDatapathButton = new JButton("Restart");
    JButton autoRunButton = new JButton("Auto Run");
    JButton stopAutoRunButton = new JButton("Pause");
    
    // Speed control for auto-run
    JLabel speedLabel = new JLabel("Speed:");
    JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 100, 3000, 1000);
    speedSlider.setPreferredSize(new Dimension(100, 25));
    speedSlider.setToolTipText("Controls auto-run delay and bus animation speed (100ms - 3000ms)");
    
    controlPanel.add(backDatapathButton);
    controlPanel.add(stepDatapathButton);
    controlPanel.add(restartDatapathButton);
    controlPanel.add(autoRunButton);
    controlPanel.add(stopAutoRunButton);
    controlPanel.add(speedLabel);
    controlPanel.add(speedSlider);
    
    // JLabel currentInstructionLabel = new JLabel("Executing: ");
    // controlPanel.add(currentInstructionLabel);
    
    // Timer for auto-run functionality
    Timer autoRunTimer = new Timer(7000, null); // Initial 7 second delay
    stopAutoRunButton.setEnabled(false); // Initially disabled
    
    // Update timer delay and animation speed when slider changes
    speedSlider.addChangeListener(changeEvent -> {
        autoRunTimer.setDelay(speedSlider.getValue());
        datapathPanel.setAnimationSpeed(speedSlider.getValue());
    });

    // Create state panel to show execution steps
    JPanel statePanel = createExecutionStatePanel();
    stepDatapathButton.addActionListener(ev -> {
        datapathPanel.setAnimationSpeed(speedSlider.getValue());
        simulator.step();
        datapathPanel.setActiveComponentsAndBuses(
            simulator.getActiveComponents(),
            simulator.getActiveBuses(),
            simulator.getBusDataValues(),
            simulator.getCurrentMicroStepDescription()
        );
        
        updateExecutionStatePanel(statePanel);
        updateStatus();
        
        // Update button states after stepping
        updateButtonStates(backDatapathButton, stepDatapathButton);
    });

    backDatapathButton.addActionListener(ev -> {
        if (simulator.canStepBack()) {
            // 1. Tell the simulator to go back one step.
            // The simulator's internal state (PC, registers, micro-step index) is now correctly restored.
            simulator.stepBack();

            // 2. Update all UI components by querying the simulator's NEW state.
            updateDatapathVisualization(); // Updates the main datapath drawing
            updateExecutionStatePanel(statePanel); // <-- USES THE CORRECT, UNIFIED METHOD
            updateStatus(); // Updates register/memory tables
            
            // 3. Update the button enabled/disabled states.
            updateButtonStates(backDatapathButton, stepDatapathButton);
        }
    });
    restartDatapathButton.addActionListener(ev -> {
        if (autoRunTimer.isRunning()) {
            autoRunTimer.stop();
            autoRunButton.setEnabled(true);
            stopAutoRunButton.setEnabled(false);
            datapathPanel.setAnimationCompletionCallback(null); // Clear callback
        }
        
        simulator.reset();
        
        datapathPanel.clearHistory();
        updateDatapathVisualization();
        
        backDatapathButton.setEnabled(simulator.canStepBack()); // Should be false at start
        stepDatapathButton.setEnabled(!simulator.getProgram().isEmpty() && !simulator.isFinished()); // Enable if program exists
        autoRunButton.setEnabled(!simulator.getProgram().isEmpty());
        
        updateExecutionStatePanel(statePanel);
        updateDatapathCodeEditor();
        updateStatus();
    });

    autoRunButton.addActionListener(ev -> {
        if (!simulator.isFinished()) {
            autoRunButton.setEnabled(false);
            stopAutoRunButton.setEnabled(true);
            stepDatapathButton.setEnabled(false);
            backDatapathButton.setEnabled(false);
            
            autoRunTimer.stop();
            
            datapathPanel.setAnimationSpeed(speedSlider.getValue());
            
            datapathPanel.setAnimationCompletionCallback(() -> {
                SwingUtilities.invokeLater(() -> {
                    if (!simulator.isFinished() && stopAutoRunButton.isEnabled()) {
                        simulator.step();
                        datapathPanel.setActiveComponentsAndBuses(
                            simulator.getActiveComponents(),
                            simulator.getActiveBuses(),
                            simulator.getBusDataValues(),
                            simulator.getCurrentMicroStepDescription()
                        );
                        updateExecutionStatePanel(statePanel);
                        updateStatus();
                    } else {
                        autoRunTimer.stop();
                        autoRunButton.setEnabled(true);
                        stopAutoRunButton.setEnabled(false);
                        stepDatapathButton.setEnabled(!simulator.isFinished());
                        backDatapathButton.setEnabled(simulator.canStepBack());
                        datapathPanel.setAnimationCompletionCallback(null); 
                    }
                });
            });
            
            simulator.step();
            datapathPanel.setActiveComponentsAndBuses(
                simulator.getActiveComponents(),
                simulator.getActiveBuses(),
                simulator.getBusDataValues(),
                simulator.getCurrentMicroStepDescription()
            );
            updateExecutionStatePanel(statePanel);
            updateStatus();
            
        } 
    });

    // Stop Auto Run functionality
    stopAutoRunButton.addActionListener(ev -> {
        autoRunTimer.stop();
        autoRunButton.setEnabled(true);
        stopAutoRunButton.setEnabled(false);
        stepDatapathButton.setEnabled(!simulator.isFinished());
        backDatapathButton.setEnabled(simulator.canStepBack());
        datapathPanel.setAnimationCompletionCallback(null); // Clear callback
    });

    // Create main layout with datapath on left and state panel on right
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.add(controlPanel, BorderLayout.NORTH);
    
    JPanel datapathCenterPanel = new JPanel(new BorderLayout());
    datapathCenterPanel.add(new JScrollPane(datapathPanel), BorderLayout.CENTER);
    datapathCenterPanel.add(statePanel, BorderLayout.EAST);
    
    mainPanel.add(datapathCenterPanel, BorderLayout.CENTER);

    // Initialize the state panel with current state
    updateExecutionStatePanel(statePanel);
    
    // Initialize the datapath code editor
    updateDatapathCodeEditor();
    
    // If program is finished, show final state
    if (simulator.isFinished()) {
        datapathPanel.setActiveComponentsAndBuses(
            simulator.getActiveComponents(),
            simulator.getActiveBuses(),
            simulator.getBusDataValues(),
            simulator.getCurrentMicroStepDescription()
        );
        updateExecutionStatePanel(statePanel);
    }

    datapathFrame.add(mainPanel);
    datapathFrame.setLocationRelativeTo(null);
    
    // Clean up timer when window is closed
    datapathFrame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
            if (autoRunTimer.isRunning()) {
                autoRunTimer.stop();
            }
        }
    });
    
    datapathFrame.setVisible(true);
        });

        // Căn giữa màn hình
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Tạo nút với màu và hiệu ứng hover
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(BUTTON_BG);
        button.setForeground(BUTTON_FG);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(BUTTON_HOVER_BG);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(BUTTON_BG);
            }
        });

        return button;
    }

    // Tùy chỉnh màu sắc cho instructionTable
    private void styleInstructionTable(JTable table) {
        table.getTableHeader().setBackground(TABLE_HEADER_BG);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setBackground(TABLE_CELL_BG);
        table.setForeground(TABLE_FG);
        table.setGridColor(Color.LIGHT_GRAY);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(row % 2 == 0 ? TABLE_CELL_BG : new Color(209, 180, 206));
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
    }

    // Tùy chỉnh màu sắc cho JTable
    private void styleTable(JTable table) {
        table.getTableHeader().setBackground(TABLE_HEADER_BG);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setBackground(TABLE_CELL_BG);
        table.setForeground(TABLE_FG);
        table.setGridColor(Color.LIGHT_GRAY);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(row % 2 == 0 ? TABLE_CELL_BG : new Color(209, 180, 206));
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
    }

    private void assembleProgram() {
        String[] lines = codeEditor.getText().split("\n");
        simulator.loadProgram(lines);
        // Execution step tracking is now handled by simulator's micro-step system
        updateStatus();
        updateInstructionTable();
        updateButtonStates();
        // Update datapath code editor if datapath window is open
        updateDatapathCodeEditor();
        if (outputArea != null) {
            outputArea.append("Program assembled with " + simulator.getProgram().size() + " instruction(s).\n");
            outputArea.append("Micro-step execution tracking enabled.\n");
        }
    }

    private void runProgram() {
        simulator.executeProgram();
        updateStatus();
        updateButtonStates();
    }

    private void restartProgram() {
        simulator.reset();

        // Execution step tracking is now handled by simulator's micro-step system
        if (datapathPanel != null) {
            datapathPanel.clearHistory();
        }
        populateInstructionTable();
        updateStatus();
        updateDatapathVisualization();
        updateButtonStates();
        simulator.getProgram().clear();
    }

    private void clearAll() {
        codeEditor.setText(""); 
        populateInstructionTable();   
        updateStatus();
        updateButtonStates();

        updateDatapathCodeEditor();
        simulator.getProgram().clear();
        simulator.reset();  
    }

    private void showHelp() {
        JOptionPane.showMessageDialog(frame,
                "Help:\n- Assemble: Load program\n- Run: Execute all\n- Step Forward/Back: Single step\n- Restart: Reset program\n- Clear All: Clear all fields\n- Datapath: Open datapath visualization\n  - Auto Run: Automatically execute all steps\n  - Speed control: Adjust auto-run delay\nSupported instructions: ADD, SUB, MOVZ, MOV, MOVK, AND, ORR, LDUR, STUR, ADDI, SUBI, B, EOR, MUL, SDIV, UDIV, LSL, LSR, ASR, CMP, SMULH, UMULH",
                "Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateStatus() {
        // Cập nhật bảng thanh ghi
        DefaultTableModel registersModel = (DefaultTableModel) registersTable.getModel();
        registersModel.setRowCount(0);
        for (int i = 0; i < 32; i++) {
            long value = simulator.getRegisterFile().readRegister(i);
            registersModel.addRow(new Object[] { "X" + i, String.format("0x%016X", value) });
        }

        // Cập nhật bảng bộ nhớ
        DefaultTableModel memoryModel = (DefaultTableModel) memoryTable.getModel();
        memoryModel.setRowCount(0);
        Memory memory = simulator.getMemory();
        if (memory != null) {
            String selectedTab = (String) memoryTabSelector.getSelectedItem();
            long baseAddress = switch (selectedTab) {
                case "Stack" -> 0x00000000L; // Giả định Stack bắt đầu từ 0xFFFFFFF0
               // case "Text" -> 0x10000000L;  // Giả định Text bắt đầu từ 0x10000000
                default -> 0L;               // Data bắt đầu từ 0x00000000
            };
            int step = 8; // Bước nhảy 8 byte (64-bit)
            int maxRows = 10; // Giới hạn số dòng hiển thị
            for (int i = 0; i < maxRows; i++) {
                long address = baseAddress + (i * step);
                try {
                    long value = memory.read(address, 8); // Sử dụng size 8 cho 64-bit
                    memoryModel.addRow(new Object[] { String.format("0x%016X", address), String.format("0x%016X", value) });
                } catch (Exception e) {
                    memoryModel.addRow(new Object[] { String.format("0x%016X", address), "N/A" });
                }
            }
        }

        // Cập nhật cờ trạng thái
        zeroFlagLabel.setText(simulator.isZeroFlag() ? "1" : "0");
        negativeFlagLabel.setText(simulator.isNegativeFlag() ? "1" : "0");
        overflowFlagLabel.setText(simulator.isOverflowFlag() ? "1" : "0");
        carryFlagLabel.setText(simulator.isCarryFlag() ? "1" : "0");

        // // Cập nhật dòng lệnh hiện tại
        // int currentPc = simulator.getPc();
        // if (currentPc >= 0 && currentPc < simulator.getProgram().size()) {
        //     Instruction currentInstruction = simulator.getProgram().get(currentPc);
        //     if (currentInstruction != null) {
        //         outputArea.append("Executing: " + currentInstruction.disassemble() + "\n");
        //     }
        // }
    }

    private void populateRegistersTable() {
        DefaultTableModel model = (DefaultTableModel) registersTable.getModel();
        for (int i = 0; i < 32; i++) {
            model.addRow(new Object[] { "X" + i, "0x0000000000000000" });
        }
    }

    private void populateMemoryTable() {
        DefaultTableModel model = (DefaultTableModel) memoryTable.getModel();
        for (int i = 0; i < 10; i++) {
            model.addRow(new Object[] { String.format("0x%016X", i * 8), "0x0000000000000000" });
        }
    }

    private void populateInstructionTable() {
        DefaultTableModel model = (DefaultTableModel) instructionTable.getModel();
        model.setRowCount(0);
    }

    private void updateInstructionTable() {
        DefaultTableModel model = (DefaultTableModel) instructionTable.getModel();
        model.setRowCount(0);
        java.util.List<Instruction> program = simulator.getProgram();
        if (program == null || program.isEmpty()) {
            System.out.println("Program is empty or null");
            return;
        }

        int lineNumber = 1;
        int address = 0;
        for (Instruction instr : program) {
            if (instr == null) {
                System.out.println("Instruction at line " + lineNumber + " is null");
                continue;
            }
            String addressStr = String.format("0x%08X", address);
            String instrStr = instr.disassemble();
            if (instrStr == null || instrStr.isEmpty()) {
                System.out.println("Disassembled instruction at line " + lineNumber + " is empty");
                instrStr = "INVALID";
            }

           // String source = instrStr;
            String meaning = "";
            try {
                String[] parts = instrStr.split("[,\\s]+");
                if (parts.length > 0) {
                    String opcode = parts[0].trim();
                    if (opcode.equals("ADD") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].trim() + " + " + parts[3].trim();
                    }
                    else if (opcode.equals("MOVK") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].replace("#", "").trim()
                                + (parts.length > 3 ? " << " + parts[3].replace("#", "").trim() : "");
                    } else if (opcode.equals("ADDS") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].trim() + " + " + parts[3].trim();
                    } else if (opcode.equals("ADDI") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].trim() + " + " + parts[3].replace("#", "").trim();
                    } else if (opcode.equals("SUB") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].trim() + " - " + parts[3].trim();
                    } else if (opcode.equals("SUBI") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].trim() + " - " + parts[3].replace("#", "").trim();
                    } else if (opcode.equals("MOVZ") && parts.length >= 3) {
                        meaning = parts[1].trim() + " = " + parts[2].replace("#", "").trim()
                                + (parts.length > 3 ? " << " + parts[4].trim() : "");
                    } else if (opcode.equals("AND") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].trim() + " & " + parts[3].trim();
                    } else if (opcode.equals("ORR") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].trim() + " | " + parts[3].trim();
                    } else if (opcode.equals("EOR") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].trim() + " ^ " + parts[3].trim();
                    } else if (opcode.equals("MUL") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].trim() + " * " + parts[3].trim();
                    } else if (opcode.equals("SDIV") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].trim() + " / " + parts[3].trim();
                    } else if (opcode.equals("UDIV") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = unsigned(" + parts[2].trim() + " / " + parts[3].trim() + ")";
                    } else if (opcode.equals("LSL") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].trim() + " << " + parts[3].replace("#", "").trim();
                    } else if (opcode.equals("LSR") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].trim() + " >>> " + parts[3].replace("#", "").trim();
                    } else if (opcode.equals("ASR") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = " + parts[2].trim() + " >> " + parts[3].replace("#", "").trim();
                    } else if (opcode.equals("CMP") && parts.length >= 3) {
                        meaning = "Compare " + parts[1].trim() + " and " + parts[2].trim();
                    } else if (opcode.equals("SMULH") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = high( signed(" + parts[2].trim() + " * " + parts[3].trim() + ") )";
                    } else if (opcode.equals("UMULH") && parts.length >= 4) {
                        meaning = parts[1].trim() + " = high( unsigned(" + parts[2].trim() + " * " + parts[3].trim() + ") )";
                    } else if (opcode.equals("LDUR") && parts.length >= 3) {
                        meaning = parts[1].trim() + " = Memory[" + parts[2].replace("[", "").replace("]", "").trim() + "]";
                    } else if (opcode.equals("STUR") && parts.length >= 3) {
                        meaning = "Memory[" + parts[2].replace("[", "").replace("]", "").trim() + "] = " + parts[1].trim();
                    } else if (opcode.equals("B") && parts.length >= 2) {
                        meaning = "Branch to " + parts[1].trim();
                    }
                    else if (opcode.equals("MOV") && parts.length >= 3) {
                        meaning = parts[1].trim() + " = " + parts[2].trim();
                    }
                    else if (opcode.equals("MOVZ") && parts.length >= 3) {
                        meaning = parts[1].trim() + " = " + parts[2].replace("#", "").trim();
                        if (parts.length >= 5 && parts[3].equalsIgnoreCase("LSL")) {
                            meaning += " << " + parts[4].replace("#", "").trim();
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error parsing instruction at line " + lineNumber + ": " + e.getMessage());
                meaning = "ERROR";
            }

            model.addRow(new Object[] { lineNumber, addressStr, instrStr, meaning });
            address += 4;
            lineNumber++;
        }
    }
    private void updateDatapathVisualization() {
        // Update register values in DatapathPanel
        for (int i = 0; i < 32; i++) {
            long regValue = simulator.getRegisterFile().readRegister(i);
            datapathPanel.updateRegisterValue(i, regValue);
        }

        // Update memory state in DatapathPanel
        Memory memory = simulator.getMemory();
        if (memory != null) {
            Map<Long, Long> memoryState = new HashMap<>();
            String selectedTab = (String) memoryTabSelector.getSelectedItem();
            long baseAddress = switch (selectedTab) {
                case "Stack" -> 0x00000000L;
                default -> 0L;
            };
            int step = 8;
            int maxRows = 10;
            for (int i = 0; i < maxRows; i++) {
                long address = baseAddress + (i * step);
                try {
                    long value = memory.read(address, 8);
                    memoryState.put(address, value);
                    datapathPanel.updateMemoryValue(address, value);
                } catch (Exception e) {
                    // Handle memory read errors if needed
                }
            }
        }

        // Update active components and buses
        datapathPanel.setActiveComponentsAndBuses(
            simulator.getActiveComponents(),
            simulator.getActiveBuses(),
            simulator.getBusDataValues(),
            simulator.getCurrentMicroStepDescription()
        );

        // Record execution state
        boolean[] flags = {
            simulator.isZeroFlag(),
            simulator.isNegativeFlag(),
            simulator.isOverflowFlag(),
            simulator.isCarryFlag()
        };
        datapathPanel.recordExecutionState(
            simulator.getCurrentHistoryStepDescription(),
            simulator.getPc(),
            flags,
            simulator.getLastExecutedInstruction(),
            simulator.isFinished(),
            0 // Micro-step index
        );
    }

    // State panel components for tracking execution steps
    private JLabel fetchLabel;
    private JLabel decodeLabel;
    private JLabel executeLabel;
    private JLabel writeBackLabel;
    private JTable codeTable;
    private CodeLineTableModel codeTableModel;
    
    private JPanel createExecutionStatePanel() {
        JPanel statePanel = new JPanel(new BorderLayout());
        statePanel.setPreferredSize(new Dimension(300, 600));
        statePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.GRAY), 
            "Code Execution", 
            TitledBorder.CENTER, 
            TitledBorder.TOP
        ));
        statePanel.setBackground(Color.WHITE);
        
        // Create step indicators
        JPanel stepsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        stepsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        stepsPanel.setBackground(Color.WHITE);
        
        // Initialize step labels
        fetchLabel = createStepLabel("1. FETCH", "Fetch instruction from memory");
        decodeLabel = createStepLabel("2. DECODE", "Decode instruction and read registers");
        executeLabel = createStepLabel("3. EXECUTE", "Perform ALU operation or calculate address");
        writeBackLabel = createStepLabel("4. WRITE BACK", "Write result to register or memory");
        
        stepsPanel.add(fetchLabel);
        stepsPanel.add(decodeLabel);
        stepsPanel.add(executeLabel);
        stepsPanel.add(writeBackLabel);
        
        // Create code table for structured view
        codeTableModel = new CodeLineTableModel();
        codeTable = new JTable(codeTableModel);
        codeTable.setDefaultRenderer(Object.class, new CodeLineTableCellRenderer());
        codeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        codeTable.setRowHeight(25);
        codeTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        codeTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // Status
        codeTable.getColumnModel().getColumn(1).setPreferredWidth(50);  // Line
        codeTable.getColumnModel().getColumn(2).setPreferredWidth(200); // Instruction
        
        JScrollPane codeScroll = new JScrollPane(codeTable);
        codeScroll.setBorder(BorderFactory.createTitledBorder("Program Code"));
        codeScroll.setPreferredSize(new Dimension(300, 350));
        
        statePanel.add(stepsPanel, BorderLayout.NORTH);
        statePanel.add(codeScroll, BorderLayout.CENTER);
        
        return statePanel;
    }
    
    private JLabel createStepLabel(String stepName, String description) {
        JLabel label = new JLabel("<html><b>" + stepName + "</b><br/><small>" + description + "</small></html>");
        label.setOpaque(true);
        label.setBackground(Color.LIGHT_GRAY);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        label.setVerticalAlignment(SwingConstants.TOP);
        return label;
    }
    
    private void updateExecutionStatePanel(JPanel statePanel) {
        if (statePanel == null) return;
        
        // 1. Get the STABLE state object from the simulator's history
        ExecutionState currentState = simulator.getCurrentExecutionState();

        
        
        // If there's no state (e.g., before the first step), do nothing.
        if (currentState == null) {
            resetStepIndicators();
            updateStepDetails("Ready", 0); 
            statePanel.repaint();
            return;
        }

        // 2. Get the micro-step index FROM THE SAVED STATE
        int microStepIndex = currentState.getCurrentMicroStepIndex();
        
        // 3. Get the pipeline stage from the specific micro-step that was recorded
        PipelineStage stage = PipelineStage.NONE;
        
        if (microStepIndex >= 0 && !simulator.isFinished()) {
            List<MicroStep> stepQueue = simulator.getMicroStepManager().getMicroStepQueue();
            if (stepQueue != null && !stepQueue.isEmpty()) {
                // The recorded micro-step index represents the step that was just executed.
                // We want to show the stage of that step.
                if (microStepIndex < stepQueue.size()) {
                    stage = stepQueue.get(microStepIndex).getStage();
                } else if (microStepIndex > 0) {
                    // If the index is beyond the queue, show the last stage
                    stage = stepQueue.get(stepQueue.size() - 1).getStage();
                }
            }
        }

        String currentInstruction = currentState.getLastExecutedInstruction();

        // 4. Map the stage to a step number for your existing highlighter
        int stepNumber = 0;
        switch (stage) {
            case FETCH:
                stepNumber = 1;
                break;
            case DECODE:
                stepNumber = 2;
                break;
            case EXECUTE:
            case MEMORY_ACCESS:
                stepNumber = 3;
                break;
            case WRITE_BACK:
                stepNumber = 4;
                break;
            case NONE:
            default:
                stepNumber = 0;
                break;
        }
        
        // Debug output
        System.out.println("DEBUG: updateExecutionStatePanel - microStepIndex: " + microStepIndex + 
                          ", stage: " + stage + ", stepNumber: " + stepNumber + 
                          ", instruction: " + currentInstruction);
        System.out.println("DEBUG: Current Stage from History: " + stage + " (at index " + microStepIndex + ") -> Highlighting Step: " + stepNumber);

        // 5. Call your existing methods to update the UI
        resetStepIndicators();
        updateStepIndicator(stepNumber);
        updateStepDetails(currentInstruction, stepNumber);
        statePanel.repaint();
    }

    
    private void resetStepIndicators() {
        if (fetchLabel != null) fetchLabel.setBackground(Color.LIGHT_GRAY);
        if (decodeLabel != null) decodeLabel.setBackground(Color.LIGHT_GRAY);
        if (executeLabel != null) executeLabel.setBackground(Color.LIGHT_GRAY);
        if (writeBackLabel != null) writeBackLabel.setBackground(Color.LIGHT_GRAY);
    }
    
    private void updateStepIndicator(int step) {
        Color activeColor = new Color(144, 238, 144); // Light green
        Color completedColor = new Color(173, 216, 230); // Light blue
        
        // If execution is finished, don't show any active step
        if (simulator.isFinished()) {
             if (fetchLabel != null) fetchLabel.setBackground(completedColor);
             if (decodeLabel != null) decodeLabel.setBackground(completedColor);
             if (executeLabel != null) executeLabel.setBackground(completedColor);
             if (writeBackLabel != null) writeBackLabel.setBackground(completedColor);
             return;
        }
        
        switch (step) {
            case 1: // Fetch
                if (fetchLabel != null) fetchLabel.setBackground(activeColor);
                break;
            case 2: // Decode
                if (fetchLabel != null) fetchLabel.setBackground(completedColor);
                if (decodeLabel != null) decodeLabel.setBackground(activeColor);
                break;
            case 3: // Execute
                if (fetchLabel != null) fetchLabel.setBackground(completedColor);
                if (decodeLabel != null) decodeLabel.setBackground(completedColor);
                if (executeLabel != null) executeLabel.setBackground(activeColor);
                break;
            case 4: // Write Back
                if (fetchLabel != null) fetchLabel.setBackground(completedColor);
                if (decodeLabel != null) decodeLabel.setBackground(completedColor);
                if (executeLabel != null) executeLabel.setBackground(completedColor);
                if (writeBackLabel != null) writeBackLabel.setBackground(activeColor);
                break;
            default:
                // step is 0, no active step, so resetStepIndicators() has already done the work
                break;
        }
    }
    
    private void updateStepDetails(String instruction, int step) {
        if (codeTableModel == null) return;
        
        // Get the program from the main code editor
        String[] codeLines = codeEditor.getText().split("\n");
        
        // Get current PC to determine which line is executing
        int currentPc = simulator.getPc();
        
        // Create list of code lines with their states
        List<CodeLine> lines = new ArrayList<>();
        for (int i = 0; i < codeLines.length; i++) {
            String line = codeLines[i].trim();
            if (!line.isEmpty()) {
                State state;
                if (i == currentPc && !simulator.isFinished()) {
                    state = State.EXECUTING;
                } else if (i < currentPc) {
                    state = State.EXECUTED;
                } else {
                    state = State.PENDING;
                }
                lines.add(new CodeLine(i + 1, codeLines[i], state));
            }
        }
        
        // Update the table model
        codeTableModel.setLines(lines);
        
        // Scroll to the currently executing line
        if (currentPc >= 0 && currentPc < lines.size()) {
            SwingUtilities.invokeLater(() -> {
                try {
                    int rowToShow = -1;
                    for (int i = 0; i < lines.size(); i++) {
                        if (lines.get(i).getState() == State.EXECUTING) {
                            rowToShow = i;
                            break;
                        }
                    }
                    if (rowToShow >= 0) {
                        codeTable.scrollRectToVisible(codeTable.getCellRect(rowToShow, 0, true));
                        codeTable.setRowSelectionInterval(rowToShow, rowToShow);
                    }
                } catch (Exception e) {
                    // Ignore scrolling errors
                }
            });
        }
    }
    private void updateDatapathCodeEditor() {
        if (codeTableModel == null) return;
        
        String[] codeLines = codeEditor.getText().split("\n");
        if (codeLines.length == 1 && codeLines[0].trim().isEmpty()) {
            // Show empty state
            List<CodeLine> emptyLines = new ArrayList<>();
            emptyLines.add(new CodeLine(1, "No program loaded", State.PENDING));
            codeTableModel.setLines(emptyLines);
            return;
        }
        
        // Create code lines with PENDING state (program loaded but not started)
        List<CodeLine> lines = new ArrayList<>();
        for (int i = 0; i < codeLines.length; i++) {
            if (!codeLines[i].trim().isEmpty()) {
                lines.add(new CodeLine(i + 1, codeLines[i], State.PENDING));
            }
        }
        
        codeTableModel.setLines(lines);
    }
    
    private JButton stepButton;
    private JButton stepBackButton;
    private JButton stepForwardButton;
    
    
    private void updateButtonStates(JButton backButton, JButton stepButton) {
        backButton.setEnabled(simulator.canStepBack());
        stepButton.setEnabled(!simulator.isFinished());
    }
    
    private void updateButtonStates() {
        if (stepBackButton != null) {
            stepBackButton.setEnabled(simulator.canStepBack());
        }
        if (stepButton != null) {
            stepButton.setEnabled(!simulator.isFinished());
        }
        if (stepForwardButton != null) {
            stepForwardButton.setEnabled(simulator.canStepForward());
        }
    }
    
    
    public void loadProgram(String[] assemblyLines) {
        simulator.loadProgram(assemblyLines);
        updateDatapathVisualization(); // Show initial state
        updateButtonStates();
    }
    
    public void resetProgram() {
        simulator.reset();
        datapathPanel.clearHistory();
        updateDatapathVisualization();
        updateButtonStates();
    }

    public static void main(String[] args) {
        InstructionConfigLoader configLoader = new InstructionConfigLoader();
        if (!configLoader.loadConfig("D:/LEGv8_Simulator/src/instruction/instructions.txt")) {
            System.err.println("Failed to load instructions.txt");
            return;
        }
        CPUSimulator simulator = new CPUSimulator(configLoader);
        new LEGv8GUI(simulator);
    }

    
}