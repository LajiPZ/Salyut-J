package backend.mips;

import backend.mips.instruction.*;
import backend.mips.operand.AReg;
import backend.mips.operand.Immediate;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import utils.DoublyLinkedList;

import java.util.*;

public class MipsFunction {
    private final String name;
    private final Function irFunction;

    private MipsBlock entry;
    private MipsBlock exit;
    private LinkedList<MipsBlock> blocks;
    private int stackSize = 0;

    public MipsFunction(Function irFunction) {
        this.name = irFunction.getName();
        this.irFunction = irFunction;
        this.blocks = new LinkedList<>();
    }

    public void setStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    public void enlargeStackSize(int by) {
        stackSize += by;
    }

    public int getStackSize() {
        return stackSize;
    }

    public void setKeyBlocks(MipsBlock entry, MipsBlock exit) {
        this.entry = entry;
        this.exit = exit;
    }

    public void addBlock(MipsBlock block) {
        blocks.add(block);
    }

    public List<MipsBlock> getBlocks() {
        return blocks;
    }

    public MipsBlock getEntry() {
        return entry;
    }

    public MipsBlock getExit() {
        return exit;
    }

    public MipsFunction build(MipsModule top) {
        MipsBlock entry = new MipsBlock(irFunction.getName() + ".entry");
        MipsBlock exit = new MipsBlock(irFunction.getName() + ".exit");

        MipsBuilder builder = new MipsBuilder(top, irFunction.getBBlocks(), entry, exit);
        MipsFunction mipsFunction = this;
        mipsFunction.setKeyBlocks(entry, exit);

        if (!irFunction.getName().equals("main")) {
            // 保存调用者的frame pointer；存到被调用函数（此函数）的栈内
            entry.addInstruction(
                new Store(Mem.Align.w, AReg.fp, AReg.sp, new Immediate(-4))
            );
        }
        entry.addInstruction(
            new Calc(Calc.Op.addiu, AReg.fp, AReg.sp, new Immediate(0))
        );

        entry.addInstruction(
            builder.addParameters(irFunction.getParams())
        );


        mipsFunction.addBlock(entry);
        for (BBlock bblock : irFunction.getBBlocks()) {
            mipsFunction.addBlock(MipsBlock.build(bblock, builder));
        }
        mipsFunction.addBlock(exit);

        // 此处可以得到一个栈大小；
        // 但在后续分配PReg时，由于溢出，栈大小会变，所以不是最终大小
        mipsFunction.setStackSize(builder.getStackSize());
        // Recover $fp
        if (!irFunction.getName().equals("main")) {
            exit.addInstruction(
                new Load(Mem.Align.w, AReg.fp, AReg.fp, new Immediate(-4))
            );
        }
        exit.addInstruction(
            new Jump(Jump.Op.jr, AReg.ra)
        );

        return mipsFunction;
    }

    public void fillStackSize() {
        exit.insertBeforeLastInstruction(
            new Calc(
                Calc.Op.addiu,
                AReg.sp, AReg.sp, new Immediate(getStackSize())
            )
        );
        // 在第一个调整$fp的指令后加
        DoublyLinkedList.Node<Instruction> target = null;
        for (DoublyLinkedList.Node<Instruction> node : entry.getInstructions()) {
            Instruction inst = node.getValue();
            if (inst instanceof Calc && inst.getDefOperands().contains(AReg.fp)) {
                target = node;
                break;
            }
        }
        entry.insertAfter(new Calc(Calc.Op.addiu, AReg.sp, AReg.sp, new Immediate(-getStackSize())), target);
    }

    public static boolean isCaller(MipsFunction function) {
        for (MipsBlock block : function.getBlocks()) {
            for (DoublyLinkedList.Node<Instruction> node : block.getInstructions()) {
                Instruction inst = node.getValue();
                if (inst instanceof Jump jump && jump.isCall()) {
                    return true;
                }
            }
        }
        return false;
    }

    public String toMIPS() {
        StringBuilder stringBuilder = new StringBuilder();
        for (MipsBlock block : blocks) {
            stringBuilder.append(block.toMIPS()).append("\n");
        }
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return name;
    }
}
