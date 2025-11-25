package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.PReg;
import backend.mips.operand.VReg;

import java.util.Map;
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

    @Override
    public void replaceOperand(Operand prevOperand, Operand newOperand) {
        if (l == prevOperand) l = newOperand;
        if (r == prevOperand) r = newOperand;
    }

    @Override
    public void fillPReg(Map<VReg, PReg> colorMap) {
        fillPReg(l, colorMap);
        fillPReg(r, colorMap);
    }

    @Override
    public String toMIPS() {
        return op + "\t" + l.toMIPS() + "\t" + r.toMIPS();
    }
}
