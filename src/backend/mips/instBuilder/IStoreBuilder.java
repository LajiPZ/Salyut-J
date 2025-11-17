package backend.mips.instBuilder;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instruction.Calc;
import backend.mips.instruction.Instruction;
import backend.mips.instruction.Mem;
import backend.mips.instruction.Store;
import backend.mips.operand.AReg;
import backend.mips.operand.Immediate;
import backend.mips.operand.Operand;
import backend.mips.operand.VReg;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.Inst;
import java.util.LinkedList;
import java.util.List;

public class IStoreBuilder extends InstBuilder {

    public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {
        List<Instruction> res = new LinkedList<>();

        int size = inst.getOperand(0).getType().getSize();
        Mem.Align align;
        if (size == 1) {
            align = Mem.Align.b;
        } else if (size == 2) {
            align = Mem.Align.h;
        } else if (size == 4) {
            align = Mem.Align.w;
        } else {
            throw new RuntimeException("Unsupported store size");
        }

        Operand valueToStore;
        if (inst.getOperand(0) instanceof IntConstant intConstant) {
            valueToStore = new VReg();
            res.add(
                new Calc(
                    Calc.Op.addiu, valueToStore, AReg.zero, new Immediate(intConstant.getValue())
                )
            );
        } else {
            valueToStore = builder.getVRegFromValue(inst.getOperand(0));
        }

        if (inst.getOperand(1).getName().startsWith("@")) {
            // Global var
            res.add(
                new Store(
                    align,
                    valueToStore,
                    AReg.zero,
                    new Immediate(builder.getGlobalVarTag(inst.getOperand(1)))
                )
            );
        } else {
            res.add(
                new Store(
                    align,
                    valueToStore,
                    builder.getVRegFromValue(inst.getOperand(1)),
                    new Immediate(0)
                )
            );
        }
        return res;
    }
}
