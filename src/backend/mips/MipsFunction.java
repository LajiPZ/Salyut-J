package backend.mips;

import backend.mips.instruction.*;
import backend.mips.operand.AReg;
import backend.mips.operand.Immediate;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;

import java.util.*;

public class MipsFunction {
    private String name;
    private MipsBlock entry;
    private MipsBlock exit;
    private ArrayList<MipsBlock> blocks;
    private int stackSize = 0;

    public MipsFunction(String name) {
        this.name = name;
        this.blocks = new ArrayList<>();
    }

    public void setStackSize(int stackSize) {
        this.stackSize = stackSize;
    }

    public void setKeyBlocks(MipsBlock entry, MipsBlock exit) {
        this.entry = entry;
        this.exit = exit;
    }

    public void addBlock(MipsBlock block) {
        blocks.add(block);
    }

    public static MipsFunction build(Function func, Map<Function, MipsFunction> functionMap) {
        MipsBlock entry = new MipsBlock(func.getName() + ".entry");
        MipsBlock exit = new MipsBlock(func.getName() + ".exit");

        MipsBuilder builder = new MipsBuilder(functionMap, func.getBBlocks(), entry, exit);
        MipsFunction mipsFunction = new MipsFunction(func.getName());
        mipsFunction.setKeyBlocks(entry, exit);

        if (!func.getName().equals("main")) {
            // 保存调用者的frame pointer；存到被调用函数的栈内
            entry.addInstruction(
                new Store(Mem.Align.w, AReg.fp, AReg.sp, new Immediate(-4))
            );
        }
        entry.addInstruction(
            new Calc(Calc.Op.addiu, AReg.fp, AReg.sp, new Immediate(0))
        );
        builder.addParameters(func.getParams());

        mipsFunction.addBlock(entry);
        for (BBlock bblock : func.getBBlocks()) {
            mipsFunction.addBlock(MipsBlock.build(bblock, builder));
        }
        mipsFunction.addBlock(exit);

        // 此处可以得到一个栈大小；
        // 但在后续分配PReg时，由于溢出，栈大小会变，所以不是最终大小
        mipsFunction.setStackSize(builder.getStackSize());
        // Recover $fp
        if (!func.getName().equals("main")) {
            exit.addInstruction(
                new Load(Mem.Align.w, AReg.fp, AReg.fp, new Immediate(-4))
            );
        }
        exit.addInstruction(
            new Jump(Jump.Op.jr, AReg.ra)
        );

        return mipsFunction;
    }
}
