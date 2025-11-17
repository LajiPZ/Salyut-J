package backend.mips.instruction;

import backend.mips.operand.Operand;

public class LoadAddr extends Instruction {
    // TODO: This is a MARS psuedo-inst, can we eliminate it?
    Operand addr;
    Operand dest;

    public LoadAddr(Operand addr, Operand dest) {
        this.addr = addr;
        this.dest = dest;
    }
}
