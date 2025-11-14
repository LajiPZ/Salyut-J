package backend.mips.instruction;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import frontend.llvm.value.instruction.Inst;

import java.util.List;

abstract public class Instruction {

    public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {

    }
}
