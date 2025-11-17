package backend.mips.instBuilder;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instruction.Beq;
import backend.mips.instruction.Calc;
import backend.mips.instruction.Instruction;
import backend.mips.instruction.Jump;
import backend.mips.operand.AReg;
import backend.mips.operand.Immediate;
import backend.mips.operand.VReg;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.IBranch;
import frontend.llvm.value.instruction.Inst;

import java.util.List;

public class IBranchBuilder extends InstBuilder {

    public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {
        // TODO: Is this the most efficient solution?
        if (((IBranch)inst).isConditinal()) {
            MipsBlock trueBlk = builder.getMipsBlock(
                ((IBranch) inst).getTrueTarget()
            );
            MipsBlock falseBlk = builder.getMipsBlock(
                ((IBranch) inst).getFalseTarget()
            );

            MipsBlock.addEdge(block, trueBlk);
            MipsBlock.addEdge(block, falseBlk);

           if (inst.getOperand(0) instanceof IntConstant condConst) {
               VReg immReg = new VReg();
                return List.of(
                    new Calc(Calc.Op.addiu, immReg, AReg.zero, new Immediate(condConst.getValue())),
                    new Beq(Beq.Op.bne, immReg, AReg.zero, trueBlk),
                    new Jump(Jump.Op.j, falseBlk)
                );
           } else {
               VReg cond = builder.getVRegFromValue(inst.getOperand(0));
               return List.of(
                   new Beq(Beq.Op.bne, cond, AReg.zero, trueBlk),
                   new Jump(Jump.Op.j, falseBlk)
               );
           }
        } else {
            MipsBlock target = builder.getMipsBlock(
                ((IBranch) inst).getUncondTarget()
            );
            MipsBlock.addEdge(block, target);
            return List.of(
                new Jump(
                    Jump.Op.j, target
                )
            );
        }
    }
}
