package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.PReg;
import backend.mips.operand.VReg;

import java.util.Map;
import java.util.Set;

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
        return Set.of((VReg) res);
    }

    @Override
    public Set<VReg> getUseVRegs() {
        return Set.of((VReg) leftOperand, (VReg) rightOperand);
    }

    @Override
    public void replaceOperand(Operand prevOperand, Operand newOperand) {
        if (res == prevOperand) res = newOperand;
        if (leftOperand == prevOperand) leftOperand = newOperand;
        if (rightOperand == prevOperand) rightOperand = newOperand;
    }

    @Override
    public void fillPReg(Map<VReg, PReg> colorMap) {
        fillPReg(res, colorMap);
        fillPReg(leftOperand, colorMap);
        fillPReg(rightOperand, colorMap);
    }

    @Override
    public String toMIPS() {
        return op + "\t" + res + ", " + leftOperand.toMIPS() + ", " + rightOperand.toMIPS();
    }
}
