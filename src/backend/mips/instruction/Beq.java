package backend.mips.instruction;

import backend.mips.MipsBlock;
import backend.mips.operand.Operand;

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
}
