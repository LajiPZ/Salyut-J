package backend.mips.instruction;

import backend.mips.operand.Operand;

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

}
