package backend.mips.instBuilder;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instruction.Instruction;
import frontend.llvm.value.instruction.Inst;

import java.util.List;

abstract public class InstBuilder {
    abstract public List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder);
}
