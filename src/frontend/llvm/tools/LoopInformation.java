package frontend.llvm.tools;

import frontend.llvm.value.BBlock;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LoopInformation {
    private BBlock head;
    private LoopInformation parentLoop;

    private List<LoopInformation> subLoops = new LinkedList<>();
    private List<BBlock> blocks = new LinkedList<>();
    private List<BBlock> latchBlocks = new LinkedList<>(); // 循环返回开头的块
    private List<BBlock> exitBlocks = new LinkedList<>();
    private List<BBlock> exitTargetBlocks = new LinkedList<>();

    public LoopInformation(BBlock head) {
        blocks.add(head);
        this.head = head;
        this.parentLoop = null;
    }

    public BBlock getHead() {
        return head;
    }

    public List<BBlock> getBlocks() {
        return blocks;
    }

    public void addExitBlock(BBlock exit) {
        exitBlocks.add(exit);
    }

    public void addExitTargetBlock(BBlock exit) {
        exitTargetBlocks.add(exit);
    }

    public void addLatchBlock(BBlock latch) {
        latchBlocks.add(latch);
    }

    public boolean hasParentLoop() {
        return parentLoop != null;
    }

    public LoopInformation getParentLoop() {
        return parentLoop;
    }

    public void setParentLoop(LoopInformation parentLoop) {
        this.parentLoop = parentLoop;
    }

    public void addSubLoop(LoopInformation subLoop) {
        subLoops.add(subLoop);
    }

    public void addBlock(BBlock block) {
        blocks.add(block);
    }

    public void reverseBlocks() {
        BBlock first = blocks.remove(0);
        Collections.reverse(blocks);
        blocks.add(0, first);
    }

    public void reverseSubLoops() {
        Collections.reverse(subLoops);
    }

    public List<LoopInformation> getSubLoops() {
        return subLoops;
    }

    public List<BBlock> getLatchBlocks() {
        return latchBlocks;
    }

    public void insertBefore(BBlock loc, BBlock newBlock) {
        int index = blocks.indexOf(loc);
        blocks.add(index, newBlock);
    }

}
