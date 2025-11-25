package backend.mips.operand;

import utils.Counter;

public class VReg extends Operand {
    // Virtual registers
    // Serves as an immediate representation in register allocation
    private static final Counter counter = new Counter();
    private int id;

    public VReg() {
        this.id = counter.get();
    }

    @Override
    public String toMIPS() {
        throw new RuntimeException("VReg shouldn't in final result");
    }
}
