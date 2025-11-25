package backend.mips.instruction;

import backend.mips.operand.CP1Reg;
import backend.mips.operand.Operand;
import backend.mips.operand.VReg;

import java.util.Set;

public class CP1RegMove extends Instruction{
    public enum Op {
        mfc1, SEP, mtc1,
    }

    private Op op;
    private Operand operand;
    private CP1Reg cp1Reg;


    public CP1RegMove(Op op, Operand operand, CP1Reg cp1Reg) {
        assert op != Op.SEP;
        this.op = op;
        this.operand = operand;
        this.cp1Reg = cp1Reg;
    }

    @Override
    public Set<VReg> getDefVRegs() {
        if (op.ordinal() < RegMove.Op.SEP.ordinal()) return Set.of((VReg) operand);
        return Set.of();
    }

    @Override
    public Set<VReg> getUseVRegs() {
        if (op.ordinal() > RegMove.Op.SEP.ordinal()) return Set.of((VReg) operand);
        return Set.of();
    }

    @Override
    public void replaceOperand(Operand prevOperand, Operand newOperand) {
        // TODO
    }
}
