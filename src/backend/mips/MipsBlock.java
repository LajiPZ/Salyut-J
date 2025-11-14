package backend.mips;

import backend.mips.instruction.Instruction;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Value;
import frontend.llvm.value.instruction.Inst;

import java.util.ArrayList;
import java.util.List;

public class MipsBlock {

    private String name;
    private ArrayList<Instruction> instructions;

    public MipsBlock(String name) {
        this.name = name;
        this.instructions = new ArrayList<>();
    }

    public void addInstruction(Instruction instruction) {
        this.instructions.add(instruction);
    }

    public void addInstruction(List<Instruction> instructions) {
        this.instructions.addAll(instructions);
    }

    public MipsBlock(BBlock block) {
        // TODO
    }

    public static MipsBlock build(BBlock block, MipsBuilder builder) {
        MipsBlock mipsBlock = builder.getMipsBlock(block);
        for (Inst inst : block.getInstructions()) {
            mipsBlock.addInstruction(Instruction.build(inst, mipsBlock, builder));
        }
        return mipsBlock;
    }
}
