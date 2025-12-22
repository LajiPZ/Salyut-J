package backend.mips.instBuilder;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instruction.CP1RegMove;
import backend.mips.instruction.Instruction;
import backend.mips.instruction.Load;
import backend.mips.instruction.Mem;
import backend.mips.operand.AReg;
import backend.mips.operand.Immediate;
import backend.mips.operand.VReg;
import frontend.llvm.value.instruction.Inst;

import java.util.List;

public class ILoadBuilder extends InstBuilder {

    public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {
        VReg reg = builder.getVRegFromValue(inst);
        int size = inst.getType().getSize();
        Mem.Align align;
        if (size == 1) {
            align = Mem.Align.b;
        } else if (size == 2) {
            align = Mem.Align.h;
        } else if (size == 4) {
            align = Mem.Align.w;
        } else {
            throw new RuntimeException("Unsupported load size");
        }
        // 加载全局变量的情况；全局变量都以@开头
        // 直接从.data 起始 + 对应全局变量的offset
        if (inst.getOperand(0).getName().startsWith("@")) {
            if (builder.getGlobalVarsInCP1Reg().containsKey(inst.getOperand(0))) {
                return List.of(
                    new CP1RegMove(
                        CP1RegMove.Op.mfc1,
                        reg,
                        builder.getGlobalVarsInCP1Reg().get(inst.getOperand(0))
                    )
                );
            }
            return List.of(
                new Load(
                    align,
                    reg,
                    AReg.zero,
                    new Immediate(builder.getGlobalVarTag(inst.getOperand(0)))
                )
            );
        }
        // 其他情况下，地址（指针）存在一个IAlloc之类分到的寄存器内，因此不需要立即数
        return List.of(
          new Load(
              align,
              reg,
              builder.getVRegFromValue(inst.getOperand(0)),
              new Immediate(0)
          )
        );
    }
}
