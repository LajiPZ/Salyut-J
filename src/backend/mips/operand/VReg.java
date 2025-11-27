package backend.mips.operand;

import settings.Settings;
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
        if (!Settings.DebugConfig.printMIPSBeforePRegAlloc) throw new RuntimeException("VReg shouldn't in final result");
        return this.toString();
    }
}
