package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.VReg;

import java.util.Set;

public class Load extends Mem {

    public Load(Align align, Operand res, Operand base, Operand offset) {
        super(align, res, base, offset);
    }

    @Override
    public Set<VReg> getDefVRegs() {
        return Set.of((VReg) src);
    }

    @Override
    public Set<VReg> getUseVRegs() {
        return Set.of((VReg) base);
    }

    @Override
    public String toMIPS() {
        return "l" + align + "\t" + src.toMIPS() + ", " + offset.toMIPS() + "(" + base.toMIPS() + ")";
    }
}
