package frontend.llvm.analysis;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.tools.ControlFlowGraph;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;

import java.util.*;

public class ControlFlowAnalysis implements Pass {

    @Override
    public void run(IrModule module) {
        module.getFunctions().forEach(f -> {
            f.setCtrlFlowGraph(analyze(f));
        });
    }

    private ControlFlowGraph analyze(Function f) {
        Map<BBlock, Set<BBlock>> successors = new HashMap<>();
        Map<BBlock, Set<BBlock>> predecessors = new HashMap<>();
        Set<BBlock> visited = new HashSet<>();
        Queue<BBlock> queue = new LinkedList<>();
        if (f.getBBlocks().isEmpty()) return null;
        queue.add(f.getBBlocks().getHead().getValue()); // That is, the first BBlock

        while (!queue.isEmpty()) {
            BBlock block = queue.poll();
            if (visited.contains(block)) {
                continue;
            }
            visited.add(block);

            predecessors.putIfAbsent(block, new HashSet<>());
            successors.put(block, new HashSet<>());
            for (BBlock successor : block.getSuccessors()) {
                successors.get(block).add(successor);
                predecessors.computeIfAbsent(successor, k -> new HashSet<>()).add(block);
                queue.add(successor);
            }
        }

        return new ControlFlowGraph(successors, predecessors, visited);
    }
}
