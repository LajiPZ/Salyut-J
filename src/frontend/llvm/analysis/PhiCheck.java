package frontend.llvm.analysis;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.instruction.IPhi;
import frontend.llvm.value.instruction.Inst;

public class PhiCheck implements Pass {
    @Override
    public void run(IrModule module) {
        for (Function function : module.getFunctions()) {
            for (var bnode : function.getBBlocks()) {
                BBlock block = bnode.getValue();
                for (var inode : block.getInstructions()) {
                    Inst inst = inode.getValue();
                    if (inst instanceof IPhi) {
                        for (int i = 0; i < inst.getOperands().size(); i+=2) {
                            BBlock source = (BBlock) inst.getOperand(i);
                            if (
                                !function.getCtrlFlowGraph().getPredecessors(block).contains(source)
                            ) throw new RuntimeException("PhiCheck Failed");
                        }
                    }
                }
            }
        }
    }
}
