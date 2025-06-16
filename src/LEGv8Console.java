import instruction.InstructionConfigLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Console interface for the LEGv8 CPU Simulator.
 */
public class LEGv8Console {
    private final CPUSimulator simulator;

    public LEGv8Console(CPUSimulator simulator) {
        this.simulator = simulator;
    }

    public void runConsole() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("LEGv8 CPU Simulator Console");
        System.out.println("Commands: load, run, step, print, exit");
        System.out.println("Supported instructions: ADD, SUB, MOVZ, AND, ORR, LDUR, STUR, ADDI, SUBI, B");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim().toLowerCase();

            try {
                switch (input) {
                    case "load":
                        loadProgramFromConsole(scanner);
                        break;
                    case "run":
                        simulator.executeProgram();
                        break;
                    case "step":
                        simulator.step();
                        break;
                    case "print":
                        simulator.printState();
                        break;
                    case "exit":
                        System.out.println("Exiting...");
                        scanner.close();
                        return;
                    default:
                        System.out.println("Unknown command: " + input);
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void loadProgramFromConsole(Scanner scanner) {
        List<String> lines = new ArrayList<>();
        System.out.println("Enter LEGv8 program (end with 'end'):");
        while (true) {
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("end")) break;
            if (!line.isEmpty() && !line.startsWith("//") && !line.startsWith("#")) {
                lines.add(line);
            }
        }
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("No valid instructions provided");
        }
        simulator.loadProgram(lines.toArray(new String[0]));
    }

    public static void main(String[] args) {
        InstructionConfigLoader configLoader = new InstructionConfigLoader();
        if (!configLoader.loadConfig("D:/LEGv8_Simulator/LEGv8_Simulator/src/instruction/instructions.txt")) {
            System.err.println("Failed to load instructions.txt");
            return;
        }
        CPUSimulator simulator = new CPUSimulator(configLoader);
        LEGv8Console console = new LEGv8Console(simulator);
        console.runConsole();
    }
}

/*
   > load
   Enter LEGv8 program (end with 'end'):
   MOVZ X1, #5, LSL #0
   ADDI X2, X1, #10
   STUR X2, [X1, #3]
   LDUR X3, [X1, #3]
   B #2
   ADD X4, X1, X2
   end
   ```

3. **Kiểm tra log load**:
   - Đảm bảo các lệnh load đúng, ví dụ:
     ```
     Loading instruction: STUR X2, [X1, #8] -> Bytecode: 0xFC010028
     Program loaded with 6 instruction(s).
     ```

4. **Thực thi từng bước** (`step`):
   - `MOVZ X1, #5, LSL #0` → `X1=5`
   - `ADDI X2, X1, #10` → `X2=15`
   - `STUR X2, [X1, #8]` → `[0xD]=15`
   - `LDUR X3, [X1, #8]` → `X3=15`
   - `B #2` → Nhảy qua `ADD`, `PC=6`
   - `step` tiếp: Kết thúc.
 */