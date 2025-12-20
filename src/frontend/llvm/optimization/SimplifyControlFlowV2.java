package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.IBranch;
import frontend.llvm.value.instruction.IPhi;
import frontend.llvm.value.instruction.Inst;
import utils.DoublyLinkedList;

import java.util.*;

public class SimplifyControlFlowV2 implements Pass {
    @Override
    public void run(IrModule module) {
        for (Function function : module.getFunctions()) {
            boolean changed = true;
            while (changed) {
                changed = false;
                // 1. 简化常量条件分支
                changed |= simplifyBranches(function);
                // 2. 移除不可达块
                changed |= removeUnreachableBlocks(function);
                // 3. 合并直线基本块
                changed |= mergeBlocks(function);
            }
        }
    }

    /**
     * 将条件为常数的 IBranch 转换为简单跳转
     */
    private boolean simplifyBranches(Function f) {
        boolean changed = false;
        for (var n : f.getBBlocks()) {
            BBlock bb = n.getValue();
            if (bb.getLastInstruction() instanceof IBranch branch && branch.isConditinal()) {
                if (branch.getOperand(0) instanceof IntConstant cond) {
                    BBlock taken = (BBlock) (cond.getValue() != 0 ? branch.getOperand(1) : branch.getOperand(2));
                    BBlock notTaken = (BBlock) (cond.getValue() != 0 ? branch.getOperand(2) : branch.getOperand(1));

                    // 移除旧的分支指令，添加新的无条件跳转
                    bb.getInstructions().getTail().drop();
                    bb.addInstruction(new IBranch(taken));

                    // 维护 SSA：从不再跳转的目标块的 PHI 指令中移除来自当前块的边
                    // notTaken.removePhiSourceFrom(bb);
                    for (DoublyLinkedList.Node<Inst> node : notTaken.getInstructions()) {
                        Inst inst = node.getValue();
                        if (inst instanceof IPhi phi) {
                            var sourcePairs = phi.getSourcePairs();
                            for (int i = 0; i < sourcePairs.size(); i++) {
                                if (sourcePairs.get(i).getValue1() == bb) {
                                    phi.dropSourcePair(i);
                                    break;
                                }
                            }
                        }
                    }
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * 移除从入口块不可达的所有基本块
     */
    private boolean removeUnreachableBlocks(Function f) {
        if (f.getBBlocks().isEmpty()) return false;

        HashSet<BBlock> reachable = new HashSet<>();
        Deque<BBlock> worklist = new LinkedList<>();

        BBlock entry = f.getBBlocks().getHead().getValue();
        reachable.add(entry);
        worklist.add(entry);

        // 标准 BFS 寻找可达块
        while (!worklist.isEmpty()) {
            BBlock curr = worklist.poll();
            for (BBlock succ : curr.getSuccessors()) {
                if (reachable.add(succ)) {
                    worklist.add(succ);
                }
            }
        }

        if (reachable.size() == f.getBBlocks().getSize()) return false;

        // 移除不可达块
        var it = f.getBBlocks().iterator();
        while (it.hasNext()) {
            BBlock bb = it.next().getValue();
            if (!reachable.contains(bb)) {
                // 通知后继节点，当前块已消失，需更新 PHI
                for (BBlock succ : bb.getSuccessors()) {
                    // succ.removePhiSourceFrom(bb);
                    for (DoublyLinkedList.Node<Inst> node : succ.getInstructions()) {
                        Inst inst = node.getValue();
                        if (inst instanceof IPhi phi) {
                            var sourcePairs = phi.getSourcePairs();
                            LinkedList<Integer> droppedIndex = new LinkedList<>();
                            for (int i = 0; i < sourcePairs.size(); i++) {
                                if (bb == sourcePairs.get(i).getValue1()) {
                                    droppedIndex.add(i);
                                }
                            }
                            for (int i = droppedIndex.size() - 1; i >= 0; i--) {
                                phi.dropSourcePair(droppedIndex.get(i));
                            }
                        }
                    }
                }
                it.remove();
            }
        }
        return true;
    }

    /**
     * 合并只有一个前驱且该前驱只有一个后继的基本块
     */
    private boolean mergeBlocks(Function f) {
        boolean changed = false;
        // 重新构建前驱关系
        Map<BBlock, List<BBlock>> predMap = buildPredecessorMap(f);

        var it = f.getBBlocks().iterator();
        // if (it.hasNext()) it.next(); // 跳过入口块

        while (it.hasNext()) {
            BBlock curr = it.next().getValue();
            List<BBlock> preds = predMap.getOrDefault(curr, Collections.emptyList());

            if (preds.size() == 1) {
                BBlock pred = preds.get(0);
                // 条件：pred 的唯一后继是 curr，且 curr 没有复杂的 PHI（或者 PHI 可以退化）
                if (pred.getSuccessors().size() == 1 && pred.getSuccessors().contains(curr)) {
                    // 1. 处理 curr 开头的 PHI 指令
                    // 因为只有一个前驱，PHI 指令的值必然是该前驱对应的输入
                    Iterator<DoublyLinkedList.Node<Inst>> instIt = curr.getInstructions().iterator();
                    while (instIt.hasNext()) {
                        Inst inst = instIt.next().getValue();
                        if (inst instanceof IPhi phi) {
                            Value val = phi.getOperand(1); // 取唯一前驱的值
                            // phi.replaceAllUsesWith(val);
                            for (DoublyLinkedList.Node<Inst> node : curr.getInstructions()) {
                                Inst inst2 = node.getValue();
                                for (int i = 0; i < inst2.getOperands().size(); i++) {
                                    if (inst2.getOperands().get(i) == phi) {
                                        inst2.replaceOperand(i, val);
                                    }
                                }
                            }
                            instIt.remove(); // 移除该 PHI
                        } else {
                            break; // 遇到非 PHI 指令停止
                        }
                    }

                    // 2. 移除 pred 末尾的跳转指令
                    pred.getInstructions().getTail().drop();

                    // 3. 将 curr 的指令搬运到 pred
                    for (DoublyLinkedList.Node<Inst> node : curr.getInstructions()) {
                        pred.addInstruction(node.getValue());
                    }

                    // 4. 更新后继块对当前块的引用（原本指向 curr 的现在指向 pred）
                    for (BBlock succ : curr.getSuccessors()) {
                        // succ.replacePhiSource(curr, pred);
                        for (DoublyLinkedList.Node<Inst> node : succ.getInstructions()) {
                            Inst inst2 = node.getValue();
                            if (inst2 instanceof IPhi phi) {
                                for (int i = 0; i < inst2.getOperands().size(); i+=2) {
                                    if (inst2.getOperands().get(i) == curr) {
                                        inst2.replaceOperand(curr, pred);
                                    }
                                }
                            }
                        }
                    }

                    it.remove(); // 从函数中移除 curr
                    return true; // 结构改变，重新迭代
                }
            }
        }
        return changed;
    }

    private Map<BBlock, List<BBlock>> buildPredecessorMap(Function f) {
        Map<BBlock, List<BBlock>> map = new HashMap<>();
        for (var n : f.getBBlocks()) {
            BBlock bb = n.getValue();
            for (BBlock succ : bb.getSuccessors()) {
                map.computeIfAbsent(succ, k -> new ArrayList<>()).add(bb);
            }
        }
        return map;
    }
}