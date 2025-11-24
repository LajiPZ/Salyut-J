package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.VReg;

import java.util.Set;

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

    @Override
    public Set<VReg> getDefVRegs() {
        return Set.of((VReg) res);
    }

    @Override
    public Set<VReg> getUseVRegs() {
        if (shiftAmount instanceof VReg) {
            return Set.of((VReg) src, (VReg) shiftAmount);
        }
        return Set.of((VReg) src);
    }
}
