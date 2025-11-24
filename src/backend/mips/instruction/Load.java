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
        return Set.of();
    }

    @Override
    public Set<VReg> getUseVRegs() {
        throw new RuntimeException("abstract Mem shouldn't in PRegAlloc");
    }
}
