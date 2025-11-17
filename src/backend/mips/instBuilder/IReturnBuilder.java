package backend.mips.instBuilder;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instruction.Calc;
import backend.mips.instruction.Instruction;
import backend.mips.instruction.Jump;
import backend.mips.operand.AReg;
import backend.mips.operand.Immediate;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.Inst;

import java.util.List;

public class IReturnBuilder extends InstBuilder {

    public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {

        MipsBlock exit = builder.getExitBlock();
        MipsBlock.addEdge(block, exit);

        if (inst.getOperands().isEmpty()) {
            return List.of(
                new Jump(
                    Jump.Op.j, exit
                )
            );
        }

        // e.g. return 0
        if (inst.getOperand(0) instanceof IntConstant retVal) {
            return List.of(
                new Calc(
                    Calc.Op.addiu, AReg.v0, AReg.zero, new Immediate(retVal.getValue())
                ),
                new Jump(
                    Jump.Op.j, exit
                )
            );
        }
        // e.g. return a + b
        return List.of(
            new Calc(
                Calc.Op.addiu,
                AReg.v0,
                builder.getVRegFromValue(inst.getOperand(0)),
                new Immediate(0)
            ),
            new Jump(
                Jump.Op.j, exit
            )
        );
    }
}
