package backend.mips.instruction;

import backend.mips.operand.Operand;

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
}
