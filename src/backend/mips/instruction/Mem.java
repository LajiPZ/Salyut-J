package backend.mips.instruction;

import backend.mips.operand.Operand;

public class Mem extends Instruction {
    public enum Align {
        w, h, b
    }

    private Align align;
    private Operand src;
    private Operand base;
    private Operand offset;

    public Mem(Align align, Operand src, Operand base, Operand offset) {
        this.align = align;
        this.src = src;
        this.base = base;
        this.offset = offset;
    }
}
