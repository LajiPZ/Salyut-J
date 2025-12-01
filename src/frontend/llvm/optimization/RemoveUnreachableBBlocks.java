package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;

import java.util.Iterator;

public class RemoveUnreachableBBlocks implements Pass {
    @Override
    public void run(IrModule module) {
        for (Function f : module.getFunctions()) {
            f.getBBlocks().removeIf(block -> !f.getCtrlFlowGraph().containsBBlock(block));
        }
    }
}
