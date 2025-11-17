package backend.mips.instruction;

import backend.mips.MipsBlock;
import backend.mips.operand.Operand;

public class Jump extends Instruction{
    public enum Op {
        j, jal, jr
    }

    private Op op;
    private Operand regTarget;
    private MipsBlock blkTarget;

    public Jump(Op op, Operand regTarget) {
        this.op = op;
        this.regTarget = regTarget;
        this.blkTarget = null;
    }

    public Jump(Op op, MipsBlock blkTarget) {
        this.op = op;
        this.blkTarget = blkTarget;
        this.regTarget = null;
    }
}
