package backend.mips.instBuilder;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instruction.Calc;
import backend.mips.instruction.Instruction;
import backend.mips.operand.AReg;
import backend.mips.operand.Immediate;
import backend.mips.operand.VReg;
import frontend.datatype.PointerType;
import frontend.llvm.value.instruction.Inst;

import java.util.List;

public class IAllocBuilder extends InstBuilder {

    public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {
        // offset对应字节；必须是4的倍数（对应一个字）
        int offset = builder.enlargeStack(
            (((PointerType) inst.getType()).getBaseType().getSize() + 3) & ~0b0011
        );
        VReg reg = builder.getVRegFromValue(inst);
        return List.of(
            new Calc(
                Calc.Op.addiu, reg, AReg.fp, new Immediate(offset)
            )
        );
    }
}
