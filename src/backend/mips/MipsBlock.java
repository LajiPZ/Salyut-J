package backend.mips;

import backend.mips.instruction.Instruction;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.instruction.Inst;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MipsBlock {
    private String name;
    private ArrayList<Instruction> instructions;

    // Control flow...
    private Set<MipsBlock> predecessors = new HashSet<>();
    private Set<MipsBlock> successors = new HashSet<>();

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
        this(".L" + block.getName());
    }

    public static void addEdge(MipsBlock from, MipsBlock to) {
        from.successors.add(to);
        to.predecessors.add(from);
    }

    public static MipsBlock build(BBlock block, MipsBuilder builder) {
        MipsBlock mipsBlock = builder.getMipsBlock(block);
        for (Inst inst : block.getInstructions()) {
            mipsBlock.addInstruction(Instruction.build(inst, mipsBlock, builder));
        }
        return mipsBlock;
    }

    public boolean isMainEntry() {
        return name.equals("main.entry");
    }
}
