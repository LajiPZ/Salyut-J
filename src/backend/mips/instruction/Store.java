package backend.mips.instruction;

import backend.mips.operand.Operand;

public class Store extends Mem {


    public Store(Align align, Operand src, Operand base, Operand offset) {
        super(align, src, base, offset);
    }
}
