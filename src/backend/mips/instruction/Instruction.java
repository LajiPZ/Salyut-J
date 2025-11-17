package backend.mips.instruction;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instBuilder.IAllocBuilder;
import backend.mips.instBuilder.InstBuilder;
import frontend.llvm.value.instruction.IAllocate;
import frontend.llvm.value.instruction.Inst;

import java.util.ArrayList;
import java.util.List;

abstract public class Instruction {

    public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        // 1. pre
        // TODO
        // 2. actual
        instructions.addAll(
            InstBuilder.build(inst, block, builder)
        );
        return instructions;
    }
}
