package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.VReg;

import java.util.Set;


public class Mem extends Instruction {

    public enum Align {
        w, h, b
    }

    private Align align;
    protected Operand src;
    protected Operand base;
    protected Operand offset;

    public Mem(Align align, Operand src, Operand base, Operand offset) {
        this.align = align;
        this.src = src;
        this.base = base;
        this.offset = offset;
    }

    @Override
    public Set<VReg> getDefVRegs() {
        throw new RuntimeException("abstract Mem shouldn't in PRegAlloc");
    }

    @Override
    public Set<VReg> getUseVRegs() {
        throw new RuntimeException("abstract Mem shouldn't in PRegAlloc");
    }

}
