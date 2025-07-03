import core.*;
import datapath.DatapathPanel;
import instruction.Instruction;
import instruction.InstructionConfigLoader;
import memory.*;


import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
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
        //JButton stepBackButton = createStyledButton("Step Back");
        //JButton stepForwardButton = createStyledButton("Step Forward");
        JButton restartButton = createStyledButton("Restart");
        JButton clearAllButton = createStyledButton("Clear All");
        JButton datapathButton = createStyledButton("Datapath");
        JButton helpButton = createStyledButton("Help");
        buttonPanel.add(assembleButton);
        buttonPanel.add(runButton);
        //buttonPanel.add(stepBackButton);
        //buttonPanel.add(stepForwardButton);
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
        String[] memoryTabs = { "Data", "Stack", "Text" };
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
        //stepBackButton.addActionListener(e -> stepBackProgram());
        //stepForwardButton.addActionListener(e -> stepForwardProgram());
        restartButton.addActionListener(e -> restartProgram());
        clearAllButton.addActionListener(e -> clearAll());
        helpButton.addActionListener(e -> showHelp());
        
        datapathButton.addActionListener(e -> {
    datapathFrame = new JFrame("LEGv8 Datapath Visualization");
    datapathFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    datapathFrame.setSize(1000, 700);

    datapathPanel = new DatapathPanel();

    // Create control panel with Step and Back buttons
    JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    JButton stepDatapathButton = new JButton("Step Datapath");
    JButton backDatapathButton = new JButton("Back Datapath");
    controlPanel.add(backDatapathButton);
    controlPanel.add(stepDatapathButton);
JLabel currentInstructionLabel = new JLabel("Executing: ");
controlPanel.add(currentInstructionLabel);

    stepDatapathButton.addActionListener(ev -> {
        currentInstructionLabel.setText("Executing: " + simulator.getLastExecutedInstruction());
        simulator.step();
        datapathPanel.setActiveComponentsAndBuses(
            simulator.getActiveComponents(),
            simulator.getActiveBuses(),
            simulator.getBusDataValues()
        );
            updateStatus();
            
       // String currentInstr = simulator.getLastExecutedInstruction();
      //  JOptionPane.showMessageDialog(datapathFrame, "Executing: " + currentInstr, "Current Instruction", JOptionPane.INFORMATION_MESSAGE);

    });

    backDatapathButton.addActionListener(ev -> {
currentInstructionLabel.setText("Executing: " + simulator.getLastExecutedInstruction());
        if (simulator.getPc() > 0) {
            simulator.setPc(simulator.getPc() - 2);
            simulator.step();
            datapathPanel.setActiveComponentsAndBuses(
                simulator.getActiveComponents(),
                simulator.getActiveBuses(),
                simulator.getBusDataValues()
            );
        }
    });

    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.add(controlPanel, BorderLayout.NORTH);
    mainPanel.add(new JScrollPane(datapathPanel), BorderLayout.CENTER);

    // If program is finished, show final state
    if (simulator.isFinished()) {
        datapathPanel.setActiveComponentsAndBuses(
            simulator.getActiveComponents(),
            simulator.getActiveBuses(),
            simulator.getBusDataValues()
        );
    }

    datapathFrame.add(mainPanel);
    datapathFrame.setLocationRelativeTo(null);
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
        updateStatus();
        updateInstructionTable();
        if (outputArea != null) {
            outputArea.append("Program assembled with " + simulator.getProgram().size() + " instruction(s).\n");
        }
    }

    private void runProgram() {
        try {
            simulator.executeProgram();
            updateStatus();
        } catch (Exception e) {
            outputArea.append("Error: " + e.getMessage() + "\n");
        }
    }

    // private void stepBackProgram() {
    //     if (simulator.getProgram().size() > 0 && simulator.getPc() > 0) {
    //         simulator.setPc(simulator.getPc() - 2);
    //         simulator.step();
    //         updateStatus();
    //     }
    // }

    // private void stepForwardProgram() {
    //     try {
    //         simulator.step();
    //         updateStatus();
    //     } catch (Exception e) {
    //         outputArea.append("Error: " + e.getMessage() + "\n");
    //     }
    // }

    private void restartProgram() {
        populateInstructionTable();
        updateStatus();
        if (outputArea != null) {
            outputArea.append("Program restarted.\n");
        }
        simulator.getProgram().clear();
        simulator.reset();
    }

    private void clearAll() {
        codeEditor.setText(""); 
        populateInstructionTable();   
        updateStatus();
        simulator.getProgram().clear();
        if (outputArea != null) {
            outputArea.setText(""); 
            outputArea.append("All cleared.\n");
        }
        simulator.reset();  
    }

    private void showHelp() {
        JOptionPane.showMessageDialog(frame,
                "Help:\n- Assemble: Load program\n- Run: Execute all\n- Step Forward/Back: Single step\n- Restart: Reset program\n- Clear All: Clear all fields\nSupported instructions: ADD, SUB, MOVZ, AND, ORR, LDUR, STUR, ADDI, SUBI, B, EOR, MUL, SDIV, UDIV, LSL, LSR, ASR, CMP, SMULH, UMULH",
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
                case "Stack" -> 0xFFFFFFF0L; // Giả định Stack bắt đầu từ 0xFFFFFFF0
                case "Text" -> 0x10000000L;  // Giả định Text bắt đầu từ 0x10000000
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

            String source = instrStr;
            String meaning = "";
            try {
                String[] parts = instrStr.split("[,\\s]+");
                if (parts.length > 0) {
                    String opcode = parts[0].trim();
                    if (opcode.equals("ADD") && parts.length >= 4) {
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
                }
            } catch (Exception e) {
                System.out.println("Error parsing instruction at line " + lineNumber + ": " + e.getMessage());
                meaning = "ERROR";
            }

            model.addRow(new Object[] { lineNumber, addressStr, instrStr, source, meaning });
            address += 4;
            lineNumber++;
        }
    }

    public static void main(String[] args) {
        InstructionConfigLoader configLoader = new InstructionConfigLoader();
        if (!configLoader.loadConfig("D:/LEGv8_Simulator/LEGv8_Simulator/src/instruction/instructions.txt")) {
            System.err.println("Failed to load instructions.txt");
            return;
        }
        CPUSimulator simulator = new CPUSimulator(configLoader);
        new LEGv8GUI(simulator);
    }
}