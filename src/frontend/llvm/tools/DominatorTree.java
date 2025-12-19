package frontend.llvm.tools;

import frontend.llvm.value.BBlock;

import java.util.*;

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

    public boolean isAncestor(BBlock u, BBlock v) {
        BBlock temp = v;
        while (temp != null) {
            if (temp == u) {
                return true;
            }
            temp = immediateDominators.get(temp);
        }
        return false;
    }

    public List<BBlock> getPostOrder(BBlock entry) {
        List<BBlock> postOrder = new LinkedList<>();
        HashSet<BBlock> visited = new HashSet<>();
        Stack<BBlock> stack = new Stack<>();
        stack.push(entry);
        while (!stack.isEmpty()) {
            BBlock block = stack.peek();
            if (visited.contains(block)) {
                postOrder.add(block);
                stack.pop();
                continue;
            }
            for (BBlock child : dominates.get(block)) {
                stack.push(child);
            }
            visited.add(block);
        }
        return postOrder;
    }
}
