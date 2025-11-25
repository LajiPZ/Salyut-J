package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.VReg;

import java.util.Set;

public class Store extends Mem {

    public Store(Align align, Operand src, Operand base, Operand offset) {
        super(align, src, base, offset);
    }

    @Override
    public Set<VReg> getDefVRegs() {
        return Set.of();
    }

    @Override
    public Set<VReg> getUseVRegs() {
        return Set.of((VReg) src, (VReg) base);
    }

    @Override
    public String toMIPS() {
        return "s" + align + "\t" + src.toMIPS() + ", " + offset.toMIPS() + "(" + base.toMIPS() + ")";
    }
}
