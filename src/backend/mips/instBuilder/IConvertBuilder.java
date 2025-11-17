package backend.mips.instBuilder;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instruction.Calc;
import backend.mips.instruction.Instruction;
import backend.mips.operand.AReg;
import backend.mips.operand.Immediate;
import backend.mips.operand.VReg;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.IConvert;
import frontend.llvm.value.instruction.Inst;

import java.util.List;

public class IConvertBuilder extends InstBuilder {

    public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {
        VReg reg = builder.getVRegFromValue(inst);
        if (((IConvert) inst).isTruncating()) {
            // 截取低位
            if (inst.getOperand(0) instanceof IntConstant intConstant) {
                return List.of(
                    new Calc(
                        Calc.Op.addiu,
                        reg,
                        AReg.zero,
                        new Immediate(intConstant.getValue() % (1 << inst.getType().getSize() * 8))
                    )
                );
            } else {
                return List.of(
                    new Calc(
                        Calc.Op.andi,
                        reg,
                        builder.getVRegFromValue(inst.getOperand(0)),
                        new Immediate((1 << inst.getType().getSize() * 8) - 1)
                    )
                );
            }
        } else {
            // 执行的都是无符号扩展
            if (inst.getOperand(0) instanceof IntConstant intConstant) {
                return List.of(
                    new Calc(
                        Calc.Op.addiu,
                        reg,
                        AReg.zero,
                        new Immediate(intConstant.getValue())
                    )
                );
            } else {
                // TODO: 也就相当于Move一次，那还有做的必要吗？
                return List.of(
                    new Calc(
                        Calc.Op.addiu,
                        reg,
                        builder.getVRegFromValue(inst.getOperand(0)),
                        new Immediate(0)
                    )
                );
            }
        }
    }
}
