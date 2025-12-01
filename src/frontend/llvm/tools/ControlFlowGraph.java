package frontend.llvm.tools;

import frontend.llvm.value.BBlock;

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
        return predecessors.get(block);
    }

    public Set<BBlock> getSuccessors(BBlock block) {
        return successors.get(block);
    }

    public boolean containsBBlock(BBlock block) {
        return blocks.contains(block);
    }
}
