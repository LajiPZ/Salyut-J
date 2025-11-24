package backend.mips;

import backend.mips.instruction.Branch;
import backend.mips.instruction.Instruction;
import backend.mips.instruction.Phi;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.instruction.Inst;

import java.util.*;

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

    public void insertBefore(Instruction inst,Instruction target) {
        int index = this.instructions.indexOf(target);
        this.instructions.add(index, inst);
    }

    public void insertAfter(Instruction inst,Instruction target) {
        int index = target == null ? 0 : this.instructions.indexOf(target) + 1;
        this.instructions.add(index, inst);
    }

    public void insertBeforeLastInstruction(Instruction instruction) {
        int index = this.instructions.size() - 1;
        this.instructions.add(index, instruction);
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public MipsBlock(BBlock block) {
        this(".L" + block.getName());
    }

    public static void addEdge(MipsBlock from, MipsBlock to) {
        from.successors.add(to);
        to.predecessors.add(from);
    }

    public static void removeEdge(MipsBlock from, MipsBlock to) {
        from.successors.remove(to);
        to.predecessors.remove(from);
    }

    public Set<MipsBlock> getPredecessors() {
        return predecessors;
    }

    public Set<MipsBlock> getSuccessors() {
        return successors;
    }

    public void replaceAllBranchTarget(MipsBlock oldBlk, MipsBlock newBlk) {
        for (Instruction instruction : instructions) {
            if (instruction instanceof Branch branch) {
                branch.replaceBranchTarget(oldBlk, newBlk);
            }
        }
    }

    public void removeAllPhi() {
        instructions.removeIf(instruction -> instruction instanceof Phi);
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
