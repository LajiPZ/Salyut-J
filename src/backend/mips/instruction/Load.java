package backend.mips.instruction;

import backend.mips.operand.Operand;

public class Load extends Mem {

    public Load(Align align, Operand src, Operand base, Operand offset) {
        super(align, src, base, offset);
    }
}
