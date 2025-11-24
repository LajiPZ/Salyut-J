package backend.mips.instruction;

import backend.mips.MipsBlock;
import backend.mips.operand.Operand;
import backend.mips.operand.VReg;

import java.util.Set;

public class Jump extends Branch {

    public enum Op {
        j, jal, jr
    }

    private Op op;
    private Operand regTarget;
    private MipsBlock blkTarget;

    public Jump(Op op, Operand regTarget) {
        this.op = op;
        this.regTarget = regTarget;
        this.blkTarget = null;
    }

    public Jump(Op op, MipsBlock blkTarget) {
        this.op = op;
        this.blkTarget = blkTarget;
        this.regTarget = null;
    }

    @Override
    public void replaceBranchTarget(MipsBlock oldBlk, MipsBlock newBlk) {
        if (blkTarget == oldBlk) {
            blkTarget = newBlk;
        }
    }

    @Override
    public Set<VReg> getDefVRegs() {
        return Set.of();
    }

    @Override
    public Set<VReg> getUseVRegs() {
        if (regTarget != null) return Set.of((VReg) regTarget);
        return Set.of();
    }
}
