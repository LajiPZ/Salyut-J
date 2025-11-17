package backend.mips.instruction;

import backend.mips.operand.Operand;

public class RegMove extends Instruction {
    public enum Op {
        mfhi, mflo, mthi, mtlo,
        mfc1, mtc1
    }

    private Op op;
    private Operand operand;

    public RegMove(Op op, Operand operand) {
        this.op = op;
        this.operand = operand;
    }
}
