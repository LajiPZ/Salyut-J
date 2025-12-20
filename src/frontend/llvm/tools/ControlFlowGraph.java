package frontend.llvm.tools;

import frontend.llvm.value.BBlock;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ControlFlowGraph {
    private Map<BBlock, Set<BBlock>> successors;
    private Map<BBlock, Set<BBlock>> predecessors;
    private Set<BBlock> blocks;

    public ControlFlowGraph(Map<BBlock, Set<BBlock>> successors, Map<BBlock, Set<BBlock>> predecessors, Set<BBlock> blocks) {
        this.successors = successors;
        this.predecessors = predecessors;
        this.blocks = blocks;
    }

    public Set<BBlock> getPredecessors(BBlock block) {
        return predecessors.getOrDefault(block, Set.of());
    }

    public Set<BBlock> getSuccessors(BBlock block) {
        return successors.getOrDefault(block, Set.of());
    }

    public boolean containsBBlock(BBlock block) {
        return blocks.contains(block);
    }

    public void insertBetween(BBlock block, BBlock predecessor, BBlock newBlock) {
        predecessors.get(block).remove(predecessor);
        predecessors.computeIfAbsent(newBlock, k -> new HashSet<>()).add(predecessor);
        predecessors.get(block).add(newBlock);
        successors.get(predecessor).remove(block);
        successors.get(predecessor).add(newBlock);
        successors.computeIfAbsent(newBlock, k -> new HashSet<>()).add(block);
    }
}
