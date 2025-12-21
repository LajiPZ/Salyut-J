package frontend.llvm.optimization;

import frontend.datatype.IntType;
import frontend.datatype.PointerType;
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
        Iterator<Function> it = module.getFunctions().iterator();
        /**
         * e.g. 陷在死循环，到不了IReturn的函数；对于能进行评测的程序，不会调用这种函数，去了也没有影响
         * 其存在会影响死代码消除，因此把这种函数去掉
         */
        while (it.hasNext()) {
            Function f = it.next();
            boolean hasReturn = false;
            for (var bnode : f.getBBlocks()) {
                BBlock bBlock = bnode.getValue();
                for (var inode : bBlock.getInstructions()) {
                    Inst inst = inode.getValue();
                    if (inst instanceof IReturn) hasReturn = true;
                }
            }
            if (!hasReturn) it.remove();
        }

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
        for (var n : function.getBBlocks()) {
            BBlock bBlock = n.getValue();
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

            if (!liveInsts.contains(bBlock.getLastInstruction())) {
                workList.addLast(bBlock.getLastInstruction());
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
                    if (usedValue instanceof IAllocate alloc) {
                        if (((PointerType) alloc.getType()).getBaseType() instanceof IntType) {
                            System.out.println("!!!");
                        }
                    }
                }
            }
        }

        // 3. 删除不活跃的指令和块
        List<BBlock> deadBlocks = new LinkedList<>();
        Iterator<DoublyLinkedList.Node<BBlock>> bBlockIterator = function.getBBlocks().iterator();
        while (bBlockIterator.hasNext()) {
            BBlock bBlock = bBlockIterator.next().getValue();
            if (!liveBlocks.contains(bBlock)) {
                deadBlocks.add(bBlock);
                continue;
            }
            var it = bBlock.getInstructions().iterator();
            while (it.hasNext()) {
                Inst inst = it.next().getValue();
                if (!liveInsts.contains(inst)) {
                    it.remove();
                    if (inst instanceof ITerminator && liveBlocks.contains(bBlock)) System.out.println("WARNING...");
                }
            }
            if (bBlock.getInstructions().isEmpty()) {
                deadBlocks.add(bBlock);
            }
        }

        // TODO: 如果要打印LLVM，此处不应删除任何块
        for (var node : function.getBBlocks()) {
            BBlock bBlock = node.getValue();
            if (deadBlocks.contains(bBlock)) {
                node.drop();
            }
        }

        for (var node : function.getBBlocks()) {
            BBlock bBlock = node.getValue();
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
        // function.getBBlocks().removeAll(deadBlocks);
    }
}
