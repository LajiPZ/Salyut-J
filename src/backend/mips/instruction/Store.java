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
        throw new RuntimeException("abstract Mem shouldn't in PRegAlloc");
    }

    @Override
    public Set<VReg> getUseVRegs() {
        throw new RuntimeException("abstract Mem shouldn't in PRegAlloc");
    }
}
