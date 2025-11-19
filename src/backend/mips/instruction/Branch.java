package backend.mips.instruction;

import backend.mips.MipsBlock;

abstract public class Branch extends Instruction {

    abstract public void replaceBranchTarget(MipsBlock oldBlk, MipsBlock newBlk);
}
