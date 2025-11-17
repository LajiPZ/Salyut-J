package backend.mips.instBuilder;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instruction.Instruction;
import backend.mips.instruction.Phi;
import backend.mips.operand.Immediate;
import backend.mips.operand.Operand;
import backend.mips.operand.VReg;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.IPhi;
import frontend.llvm.value.instruction.Inst;
import utils.Pair;

import java.util.List;

public class IPhiBuilder extends InstBuilder {

    public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {
        VReg reg = builder.getVRegFromValue(inst);
        Phi phi = new Phi(reg);
        for (Pair<BBlock, Value> pairs : ((IPhi) inst).getSourcePairs()) {
            Operand operand;
            if (pairs.getValue2() instanceof IntConstant intConstant) {
                operand = new Immediate(intConstant.getValue());
            } else  {
                operand = builder.getVRegFromValue(pairs.getValue2());
            }
            phi.addOperand(operand, builder.getMipsBlock(pairs.getValue1()));
        }
        return List.of(phi);
    }
}
