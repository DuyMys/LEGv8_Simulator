package memory;

import util.Constants;
import exceptions.MemoryAccessException;


public class Memory {
    private byte[] memory; 

    public Memory() {
        memory = new byte[Constants.MEMORY_SIZE];
    }

   
    public int read(long address) {
        validateAddress(address, 4);
        // Chuyển 4 byte thành int (big-endian)
        return ((memory[(int) address] & 0xFF) << 24) |
               ((memory[(int) address + 1] & 0xFF) << 16) |
               ((memory[(int) address + 2] & 0xFF) << 8) |
               (memory[(int) address + 3] & 0xFF);
    }

   
    public void writeInt(long address, int value) {
        validateAddress(address, 4);
        // Ghi 4 byte (big-endian)
        memory[(int) address] = (byte) (value >> 24);
        memory[(int) address + 1] = (byte) (value >> 16);
        memory[(int) address + 2] = (byte) (value >> 8);
        memory[(int) address + 3] = (byte) value;
    }

    
    public long readLong(long address) {
        validateAddress(address, 8);
        // Chuyển 8 byte thành long (big-endian)
        return ((long) (memory[(int) address] & 0xFF) << 56) |
               ((long) (memory[(int) address + 1] & 0xFF) << 48) |
               ((long) (memory[(int) address + 2] & 0xFF) << 40) |
               ((long) (memory[(int) address + 3] & 0xFF) << 32) |
               ((long) (memory[(int) address + 4] & 0xFF) << 24) |
               ((long) (memory[(int) address + 5] & 0xFF) << 16) |
               ((long) (memory[(int) address + 6] & 0xFF) << 8) |
               (memory[(int) address + 7] & 0xFF);
    }

    
    public void writeLong(long address, long value) {
        validateAddress(address, 8);
        // Ghi 8 byte (big-endian)
        memory[(int) address] = (byte) (value >> 56);
        memory[(int) address + 1] = (byte) (value >> 48);
        memory[(int) address + 2] = (byte) (value >> 40);
        memory[(int) address + 3] = (byte) (value >> 32);
        memory[(int) address + 4] = (byte) (value >> 24);
        memory[(int) address + 5] = (byte) (value >> 16);
        memory[(int) address + 6] = (byte) (value >> 8);
        memory[(int) address + 7] = (byte) value;
    }

    
    public void reset() {
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0;
        }
    }

    
    private void validateAddress(long address, int size) {
        if (address < 0 || address + size > Constants.MEMORY_SIZE) {
        throw new MemoryAccessException("Dia chi bo nho khong hop le: 0x" + Long.toHexString(address));
        }
        if (address % size != 0) {
            throw new MemoryAccessException("Dia chi phai can chinh theo " + size + " byte: 0x" + Long.toHexString(address));
        }
    }

    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < memory.length; i += 4) {
            int value = read(i); // Đọc 4 byte
            if (value != 0) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(String.format("0x%X: 0x%08X", i, value));
                first = false;
            }
        }
        return sb.length() > 0 ? sb.toString() : "Bo nho rong";
    }
}