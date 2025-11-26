package backend.mips.instruction;

import backend.mips.MipsBlock;
import backend.mips.operand.Operand;
import backend.mips.operand.PReg;
import backend.mips.operand.VReg;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Beq extends Branch {

    public enum Op {
        beq, bne
    }

    private Op op;
    private Operand left, right;
    private MipsBlock target;

    public Beq(Op op, Operand left, Operand right, MipsBlock target) {
        this.op = op;
        this.left = left;
        this.right = right;
        this.target = target;
    }

    @Override
    public void replaceBranchTarget(MipsBlock oldBlock, MipsBlock newBlock) {
        if (target == oldBlock) {
            target = newBlock;
        }
    }

    @Override
    public Set<VReg> getDefVRegs() {
        return Set.of();
    }

    @Override
    public Set<VReg> getUseVRegs() {
        return Set.of(left, right).stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toSet());
    }

    @Override
    public void replaceOperand(Operand prevOperand, Operand newOperand) {
        if (left == prevOperand) left = newOperand;
        if (right == prevOperand) right = newOperand;
    }

    @Override
    public void fillPReg(Map<VReg, PReg> colorMap) {
        left = fillPReg(left, colorMap);
        right = fillPReg(right, colorMap);
    }

    @Override
    public String toMIPS() {
        return op + "\t" + left.toMIPS() + ", " + right.toMIPS() + ", " + target;
    }

    @Override
    public List<Operand> getDefOperands() {
        return List.of();
    }
}
