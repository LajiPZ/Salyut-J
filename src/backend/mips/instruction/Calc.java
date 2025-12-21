package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.PReg;
import backend.mips.operand.VReg;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Calc extends Instruction {

    public enum Op {
        addu, subu,
        and, or, xor, not,
        slt, sltu, slti, sltiu,
        addiu, andi, ori, xori,
        sle, sge, sgt, seq, sne
    }

    private Op op;
    private Operand res;
    private Operand leftOperand;
    private Operand rightOperand;

    public Calc(Op op, Operand res, Operand leftOperand, Operand rightOperand) {
        this.op = op;
        this.res = res;
        this.leftOperand = leftOperand;
        this.rightOperand = rightOperand;
    }

    @Override
    public Set<VReg> getDefVRegs() {
        return Set.of(res).stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toSet());
    }

    @Override
    public Set<VReg> getUseVRegs() {
        return Stream.of(leftOperand, rightOperand).distinct().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toSet());
    }

    @Override
    public List<Operand> getDefOperands() {
        return List.of(res);
    }

    @Override
    public void replaceOperand(Operand prevOperand, Operand newOperand) {
        if (res == prevOperand) res = newOperand;
        if (leftOperand == prevOperand) leftOperand = newOperand;
        if (rightOperand == prevOperand) rightOperand = newOperand;
    }

    @Override
    public void fillPReg(Map<VReg, PReg> colorMap) {
        res = fillPReg(res, colorMap);
        leftOperand = fillPReg(leftOperand, colorMap);
        rightOperand = fillPReg(rightOperand, colorMap);
    }

    @Override
    public String toMIPS() {
        return op + "\t" + res.toMIPS() + ", " + leftOperand.toMIPS() + ", " + rightOperand.toMIPS();
    }

    public Op getOp() {
        return op;
    }

    public Operand[] getOperands() {
        return new Operand[]{res, leftOperand, rightOperand};
    }
}
