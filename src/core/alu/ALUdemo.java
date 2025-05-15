package legv8.util;

public class ALUDemo {
    public static void main(String[] args) {
        ALU alu = new ALU();
        ALUResult result = alu.compute(ALUOperation.SUB, 5, 3);

        System.out.println("Kết quả phép SUB(5, 3):");
        System.out.println(result);  // Sẽ in theo định dạng toString() trong ALUResult
    }
}
