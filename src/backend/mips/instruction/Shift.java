package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.PReg;
import backend.mips.operand.VReg;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Shift extends Instruction {

    public enum Op {
        sll, srl, sra, sllv, srlv, srav
    }

    private Op op;
    private Operand res;
    private Operand src, shiftAmount;

    public Shift(Op op, Operand res, Operand src, Operand shiftAmount) {
        this.op = op;
        this.res = res;
        this.src = src;
        this.shiftAmount = shiftAmount;
    }

    @Override
    public Set<VReg> getDefVRegs() {
        return Set.of(res).stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toSet());
    }

    @Override
    public Set<VReg> getUseVRegs() {
        if (shiftAmount instanceof VReg) {
            return Set.of(src, shiftAmount).stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toSet());
        }
        return Set.of(src).stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toSet());
    }

    @Override
    public void replaceOperand(Operand prevOperand, Operand newOperand) {
        if (res == prevOperand) res = newOperand;
        if (src == prevOperand) src = newOperand;
        if (shiftAmount == prevOperand) shiftAmount = newOperand;
    }

    @Override
    public void fillPReg(Map<VReg, PReg> colorMap) {
        res = fillPReg(res, colorMap);
        src = fillPReg(src, colorMap);
        shiftAmount = fillPReg(shiftAmount, colorMap);
    }

    @Override
    public String toMIPS() {
        return op + "\t" + res.toMIPS() + ", " + src.toMIPS() + ", " + shiftAmount.toMIPS();
    }
}
