package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.PReg;
import backend.mips.operand.VReg;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RegMove extends Instruction {

    public enum Op {
        mfhi, mflo, SEP,
        mthi, mtlo,
    }

    private Op op;
    private Operand operand;


    public RegMove(Op op, Operand operand) {
        assert op != Op.SEP;
        this.op = op;
        this.operand = operand;
    }

    @Override
    public Set<VReg> getDefVRegs() {
        if (op.ordinal() < Op.SEP.ordinal()) return Set.of(operand).stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toSet());
        return Set.of();
    }

    @Override
    public Set<VReg> getUseVRegs() {
        if (op.ordinal() > Op.SEP.ordinal()) return Set.of(operand).stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toSet());
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
        return op + "\t" + operand.toMIPS();
    }
}
