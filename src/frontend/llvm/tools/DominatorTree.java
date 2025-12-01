package frontend.llvm.tools;

import frontend.llvm.value.BBlock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DominatorTree {
    private Map<BBlock, Set<BBlock>> dominatedBy = new HashMap<>();
    private Map<BBlock, List<BBlock>> dominates = new HashMap<>();
    private Map<BBlock, BBlock> immediateDominators = new HashMap<>();

    public DominatorTree(
        Map<BBlock, Set<BBlock>> dominatedBy,
        Map<BBlock, List<BBlock>> dominates,
        Map<BBlock, BBlock> immediateDominators
    ) {
        this.dominatedBy.putAll(dominatedBy);
        this.dominates.putAll(dominates);
        this.immediateDominators.putAll(immediateDominators);
    }

    public BBlock getImmediateDominator(BBlock block) {
        return immediateDominators.get(block);
    }
}
