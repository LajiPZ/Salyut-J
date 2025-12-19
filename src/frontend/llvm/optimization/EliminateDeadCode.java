package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.tools.UseRecord;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;
import frontend.llvm.value.instruction.*;
import utils.DoublyLinkedList;

import java.util.*;

public class EliminateDeadCode implements Pass {
    @Override
    public void run(IrModule module) {
        for (Function f : module.getFunctions()) {
            execute(f);
        }
    }

    private void execute(Function function) {
        HashSet<Inst> liveInsts = new HashSet<>();
        HashSet<BBlock> liveBlocks = new HashSet<>();
        Deque<Inst> workList = new LinkedList<>();
        HashMap<Inst, BBlock> blockMap = new HashMap<>();

        // 1. Init
        for (BBlock bBlock : function.getBBlocks()) {
            for (DoublyLinkedList.Node<Inst> node : bBlock.getInstructions()) {
                Inst inst = node.getValue();
                blockMap.put(inst, bBlock);
                if (inst instanceof IStore || inst instanceof ICall || inst instanceof IReturn) {
                    if (!workList.contains(inst)) {
                        workList.add(inst);
                    }
                }
            }
        }

        // 2. 扩散“活跃属性”
        while (!workList.isEmpty()) {
            Inst inst = workList.pop();
            BBlock bBlock = blockMap.get(inst);
            liveInsts.add(inst);
            liveBlocks.add(bBlock);

            if (inst instanceof IPhi phi) {
                for (var sourcePair : phi.getSourcePairs()) {
                    BBlock source = sourcePair.getValue1();
                    if (!liveBlocks.contains(source)) {
                        workList.add(source.getLastInstruction());
                    }
                    liveBlocks.add(source);
                }
            }

            for (BBlock predecessor : function.getCtrlFlowGraph().getPredecessors(bBlock)) {
                if (!liveInsts.contains(predecessor.getLastInstruction())) {
                    workList.add(predecessor.getLastInstruction());
                }
            }

            for (UseRecord use : inst.getUses()) {
                Value usedValue = use.getValue();
                if (!(usedValue instanceof Inst)) continue;
                if (!liveInsts.contains(usedValue)) {
                    workList.add((Inst) usedValue);
                }
            }
        }

        // 3. 删除不活跃的指令和块
        List<BBlock> deadBlocks = new LinkedList<>();
        Iterator<BBlock> bBlockIterator = function.getBBlocks().iterator();
        while (bBlockIterator.hasNext()) {
            BBlock bBlock = bBlockIterator.next();
            var it = bBlock.getInstructions().iterator();
            while (it.hasNext()) {
                Inst inst = it.next().getValue();
                if (!liveInsts.contains(inst)) {
                    it.remove();
                }
            }
            if (bBlock.getInstructions().isEmpty()) {
                bBlockIterator.remove();
                deadBlocks.add(bBlock);
            }
        }

        for (BBlock bBlock : function.getBBlocks()) {
            ITerminator terminator = (ITerminator) bBlock.getLastInstruction();
            if (terminator.getSuccessors().stream().anyMatch(deadBlocks::contains)) {
                if (terminator.getSuccessors().size() != 2) {
                    throw new RuntimeException("Only CondBranch can be dealt with at DeadCodeElimination");
                }
                BBlock successor = terminator.getSuccessors().stream()
                    .filter(Objects::nonNull)
                    .filter(b -> !deadBlocks.contains(b))
                    .findFirst().orElseThrow();
                bBlock.getInstructions().getTail().drop();
                bBlock.addInstruction(new IBranch(successor));
            }
        }

        // TODO: is it safe?
        function.getBBlocks().removeAll(deadBlocks);
    }
}
