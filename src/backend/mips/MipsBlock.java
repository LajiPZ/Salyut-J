package backend.mips;

import backend.mips.instruction.Branch;
import backend.mips.instruction.Instruction;
import backend.mips.instruction.Phi;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.instruction.Inst;
import utils.Counter;
import utils.DoublyLinkedList;

import java.util.*;

public class MipsBlock {
    private String name;
    private DoublyLinkedList<Instruction> instructions;

    // Control flow...
    private Set<MipsBlock> predecessors = new HashSet<>();
    private Set<MipsBlock> successors = new HashSet<>();

    private static Counter counter = new Counter();

    public MipsBlock(String name) {
        this.name = name;
        this.instructions = new DoublyLinkedList<>();
    }

    public void addInstruction(Instruction instruction) {
        new DoublyLinkedList.Node<>(instruction).insertIntoTail(this.instructions);
    }

    public void addInstruction(List<Instruction> instructions) {
        for (Instruction instruction : instructions) {
            new DoublyLinkedList.Node<>(instruction).insertIntoTail(this.instructions);
        }
    }

    public void insertBefore(Instruction inst, DoublyLinkedList.Node<Instruction> target) {
        new DoublyLinkedList.Node<>(inst).insertBefore(target);
    }

    public void insertAfter(Instruction inst, DoublyLinkedList.Node<Instruction> target) {
        if (target == null) {
            new DoublyLinkedList.Node<>(inst).insertIntoHead(this.instructions);
        } else {
            new DoublyLinkedList.Node<>(inst).insertAfter(target);
        }
    }

    public void insertBeforeLastInstruction(Instruction instruction) {
        new DoublyLinkedList.Node<>(instruction).insertBefore(this.instructions.getTail());
    }

    public DoublyLinkedList<Instruction> getInstructions() {
        return instructions;
    }

    public MipsBlock(BBlock block) {
        this(".L" + counter.get() + "_IR_" +  block.getName());
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
        for (DoublyLinkedList.Node<Instruction> node : instructions) {
            Instruction inst = node.getValue();
            if (inst instanceof Branch branch) {
                branch.replaceBranchTarget(oldBlk, newBlk);
            }
        }
    }

    public void removeAllPhi() {
        Iterator<DoublyLinkedList.Node<Instruction>> it = instructions.iterator();
        while (it.hasNext()) {
            DoublyLinkedList.Node<Instruction> node = it.next();
            Instruction inst = node.getValue();
            if (inst instanceof Phi) { it.remove(); }
        }
    }

    public static MipsBlock build(BBlock block, MipsBuilder builder) {
        MipsBlock mipsBlock = builder.getMipsBlock(block);
        for (DoublyLinkedList.Node<Inst> node : block.getInstructions()) {
            Inst inst = node.getValue();
            mipsBlock.addInstruction(Instruction.build(inst, mipsBlock, builder));
        }
        return mipsBlock;
    }

    public boolean isMainEntry() {
        return name.equals("main.entry");
    }

    public boolean isMainExit() {
        return name.equals("main.exit");
    }

    public String toMIPS() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  ").append(name).append(":\n");
        if (isMainExit()) {
            // 直接退出即可，忽略退出块内对栈等的操作
            stringBuilder.append("    li $v0, 10\n");
            stringBuilder.append("    syscall\n");
        } else {
            for (DoublyLinkedList.Node<Instruction> node : instructions) {
                Instruction instruction = node.getValue();
                stringBuilder.append("  ");
                stringBuilder.append(instruction.toMIPS()).append("\n");
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return name;
    }
}
