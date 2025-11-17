package backend.mips.instBuilder;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instruction.Instruction;
import frontend.llvm.value.instruction.*;

import java.util.List;

abstract public class InstBuilder {
     public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {
         if (inst instanceof IAllocate) return IAllocBuilder.build(inst, block, builder);
         if (inst instanceof ICalc || inst instanceof ICompare) return ICalcBuilder.build(inst, block, builder);
         if (inst instanceof IBranch) return IBranchBuilder.build(inst, block, builder);
         if (inst instanceof ICall) return ICallBuilder.build(inst, block, builder);
         if (inst instanceof IReturn) return IReturnBuilder.build(inst, block, builder);
         if (inst instanceof ILoad) return ILoadBuilder.build(inst, block, builder);
         if (inst instanceof IStore) return IStoreBuilder.build(inst, block, builder);
         if (inst instanceof IConvert) return IConvertBuilder.build(inst, block, builder);
         if (inst instanceof IPhi) return IPhiBuilder.build(inst, block, builder);
         if (inst instanceof IGep) return IGepBuilder.build(inst, block, builder);
         return null;
     }
}
