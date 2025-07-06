import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.AbstractTableModel;
// Enum to represent the status of a code line
enum State {
    PENDING,
    EXECUTING,
    EXECUTED
}

// Class to represent a code line with its status
class CodeLine {
    private final int lineNumber;
    private final String instruction;
    private State state;

    public CodeLine(int lineNumber, String instruction, State state) {
        this.lineNumber = lineNumber;
        this.instruction = instruction;
        this.state = state;
    }

    public int getLineNumber() { return lineNumber; }
    public String getInstruction() { return instruction; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; }
}

// Custom table model for code lines
class CodeLineTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Status", "Line", "Instruction"};
    private List<CodeLine> lines = new ArrayList<>();

    public void setLines(List<CodeLine> lines) {
        this.lines = lines;
        fireTableDataChanged();
    }

    public void updateState(int rowIndex, State newState) {
        if (rowIndex >= 0 && rowIndex < lines.size()) {
            lines.get(rowIndex).setState(newState);
            fireTableRowsUpdated(rowIndex, rowIndex);
        }
    }

    @Override
    public int getRowCount() {
        return lines.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public CodeLine getCodeLineAt(int rowIndex) {
        return lines.get(rowIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        CodeLine line = lines.get(rowIndex);
        switch (columnIndex) {
            case 0: // Status
                switch (line.getState()) {
                    case EXECUTING: return "▶";
                    case EXECUTED:  return "✓";
                    default:        return "";
                }
            case 1: // Line Number
                return line.getLineNumber();
            case 2: // Instruction
                return line.getInstruction();
            default:
                return null;
        }
    }
}

// Custom cell renderer for highlighting code lines
class CodeLineTableCellRenderer extends DefaultTableCellRenderer {
    private static final Color EXECUTING_COLOR = new Color(255, 255, 180); // Light Yellow
    private static final Color EXECUTED_COLOR = new Color(210, 240, 210);  // Light Green
    private static final Color PENDING_COLOR = Color.WHITE;
    private static final Color EXECUTED_TEXT_COLOR = Color.GRAY;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        CodeLineTableModel model = (CodeLineTableModel) table.getModel();
        CodeLine line = model.getCodeLineAt(row);

        // Customize appearance based on the line's state
        switch (line.getState()) {
            case EXECUTING:
                c.setBackground(EXECUTING_COLOR);
                c.setForeground(Color.BLACK);
                c.setFont(c.getFont().deriveFont(Font.BOLD));
                break;
            case EXECUTED:
                c.setBackground(EXECUTED_COLOR);
                c.setForeground(EXECUTED_TEXT_COLOR);
                c.setFont(c.getFont().deriveFont(Font.ITALIC));
                break;
            case PENDING:
            default:
                c.setBackground(PENDING_COLOR);
                c.setForeground(Color.BLACK);
                c.setFont(c.getFont().deriveFont(Font.PLAIN));
                break;
        }
        
        // Handle selection color override
        if (isSelected) {
            c.setBackground(table.getSelectionBackground());
            c.setForeground(table.getSelectionForeground());
        }

        // Center align the status and line number columns
        if (column == 0 || column == 1) {
            ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
        } else {
            ((JLabel) c).setHorizontalAlignment(SwingConstants.LEFT);
        }

        return c;
    }
}
