package core;
/**
 * Controls access to the LEGv8 register file.
 */
public class RegisterFileController {
    private final RegisterStorage storage;

    public RegisterFileController(RegisterStorage storage) {
        this.storage = storage;
    }

    public long readRegister(int index) {
        return storage.read(index);
    }

    public void reset()
    {
        storage.reset();
    }

    public void writeRegister(int index, long value, boolean regWrite) {
        if (regWrite && index != 31) {
            storage.write(index, value);
        }
    }

    
}