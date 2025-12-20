package frontend.llvm.analysis;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.tools.ControlFlowGraph;
import frontend.llvm.tools.DominatorTree;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import utils.DoublyLinkedList;

import java.util.*;

public class DominatorAnalysis implements Pass {

    @Override
    public void run(IrModule module) {
        for (Function f : module.getFunctions()) {
            f.setDomTree(analyze(f));
        }
    }

    private DominatorTree analyze(Function f) {
        Map<BBlock, Set<BBlock>> dominatedBy = new HashMap<>();
        Map<BBlock, List<BBlock>> dominates = new HashMap<>();
        HashMap<BBlock, BBlock> immediateDominators = new HashMap<>();

        if (f.getBBlocks().isEmpty()) return null;

        for (DoublyLinkedList.Node<BBlock> node : f.getBBlocks()) {
            BBlock blk = node.getValue();
            dominatedBy.put(blk, new HashSet<>());
            dominates.put(blk, new LinkedList<>());
        }

        for (var node : f.getBBlocks()) {
            BBlock blk = node.getValue();
            Set<BBlock> visited = new HashSet<>() {{
                add(blk);
            }};
            dfs(f.getBBlocks().getHead().getValue(), f.getCtrlFlowGraph(), visited);
            // 移除Blk之后，不能到，则被blk支配；注意此时已经删掉了不可达块
            for (var node1 : f.getBBlocks()) {
                BBlock t = node1.getValue();
                if (!visited.contains(t)) {
                    dominatedBy.get(t).add(blk);
                }
            }
        }

        for (var entry : dominatedBy.entrySet()) {
            BBlock blk = entry.getKey();
            Set<BBlock> dominatorSet = entry.getValue();
            for (BBlock dominator : dominatorSet) {
                if (dominator == blk) {
                    continue;
                }
                boolean isImmediateDominator = true;
                for (BBlock other : dominatorSet) {
                    if (other != blk && other != dominator && dominatedBy.get(other).contains(dominator)) {
                        isImmediateDominator = false;
                        break;
                    }
                }
                if (isImmediateDominator) {
                    immediateDominators.put(blk, dominator);
                    dominates.get(dominator).add(blk);
                    break;
                }
            }
        }

        return new DominatorTree(dominatedBy, dominates, immediateDominators);
    }

    private void dfs(BBlock blk, ControlFlowGraph ctrlFlowGraph, Set<BBlock> visited) {
        if (visited.contains(blk)) {
            return;
        }
        visited.add(blk);
        for (BBlock successor : blk.getSuccessors()) {
            dfs(successor, ctrlFlowGraph, visited);
        }
    }
}
