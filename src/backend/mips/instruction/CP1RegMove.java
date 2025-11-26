package backend.mips.instruction;

import backend.mips.operand.CP1Reg;
import backend.mips.operand.Operand;
import backend.mips.operand.PReg;
import backend.mips.operand.VReg;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CP1RegMove extends Instruction{
    public enum Op {
        mfc1, SEP, mtc1,
    }

    private Op op;
    private Operand operand;
    private CP1Reg cp1Reg;


    public CP1RegMove(Op op, Operand operand, CP1Reg cp1Reg) {
        assert op != Op.SEP;
        this.op = op;
        this.operand = operand;
        this.cp1Reg = cp1Reg;
    }

    @Override
    public Set<VReg> getDefVRegs() {
        if (op.ordinal() < RegMove.Op.SEP.ordinal()) return Set.of(operand).stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toSet());
        return Set.of();
    }

    @Override
    public Set<VReg> getUseVRegs() {
        if (op.ordinal() > RegMove.Op.SEP.ordinal()) return Set.of(operand).stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toSet());
        return Set.of();
    }

    @Override
    public void replaceOperand(Operand prevOperand, Operand newOperand) {
        if (operand == prevOperand) operand = newOperand;
    }

    @Override
    public void fillPReg(Map<VReg, PReg> colorMap) {
        operand = fillPReg(operand, colorMap);
    }

    @Override
    public String toMIPS() {
        return op + "\t" + operand.toMIPS() + ", " + cp1Reg.toMIPS();
    }
}
