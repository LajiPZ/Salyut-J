package backend.mips.instruction;

import backend.mips.operand.Operand;

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
}
