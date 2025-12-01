package backend.mips.utils.spillLoc;

import backend.mips.operand.CP1Reg;

public class CP1SpillLoc extends SpillLoc {
    private CP1Reg reg;

    public CP1SpillLoc(CP1Reg reg) {
        this.reg = reg;
    }

    public CP1Reg getReg() {
        return reg;
    }

    @Override
    public String toString() {
        return "CP1SpillLoc [reg=" + reg.toMIPS() + "]";
    }
}
