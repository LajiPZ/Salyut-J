package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.VReg;

import java.util.Set;

public class MulDiv extends Instruction {

    public enum Op {
        mult, multu, div, divu
    }

    private Op op;
    private Operand l,r;

    public MulDiv(Op op, Operand l, Operand r) {
        this.op = op;
        this.l = l;
        this.r = r;
    }

    @Override
    public Set<VReg> getDefVRegs() {
        return Set.of();
    }

    @Override
    public Set<VReg> getUseVRegs() {
        return Set.of((VReg) l, (VReg) r);
    }
}
