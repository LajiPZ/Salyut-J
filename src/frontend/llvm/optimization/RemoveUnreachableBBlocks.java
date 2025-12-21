package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.instruction.Inst;
import utils.DoublyLinkedList;

import java.util.Iterator;

public class RemoveUnreachableBBlocks implements Pass {
    @Override
    public void run(IrModule module) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Function f : module.getFunctions()) {
                for (var node : f.getBBlocks()) {
                    BBlock block = node.getValue();
                    if (!f.getCtrlFlowGraph().containsBBlock(block)) {
                        node.drop();
                        for (BBlock blk : f.getCtrlFlowGraph().getPredecessors(block)) {
                            f.getCtrlFlowGraph().getSuccessors(blk).remove(block);
                        }
                        for (BBlock blk : f.getCtrlFlowGraph().getSuccessors(block)) {
                            f.getCtrlFlowGraph().getPredecessors(blk).remove(block);
                        }
                        changed = true;
                    }
                }
                // f.getBBlocks().removeIf(block -> !f.getCtrlFlowGraph().containsBBlock(block));
            }
        }

    }
}
