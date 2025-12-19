package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.tools.LoopInformation;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;
import frontend.llvm.value.instruction.*;
import utils.DoublyLinkedList;

import java.util.*;

public class LoopHoist implements Pass {

    private List<LoopInformation> loopsToHoist = new LinkedList<>();

    @Override
    public void run(IrModule module) {
        for (Function f : module.getFunctions()) {
            loopsToHoist.clear();
            Value.counter.set(f.resumeValCounter());
            execute(f);
            f.saveCurrentValCounter(Value.counter.reset());
        }
        // TODO: 重跑分析？
    }

    private void execute(Function function) {
        BBlock currentHoistedBlock;

        // 1. 后序遍历循环；效果是，先处理内层循环，再处理外层
        for (LoopInformation loop : function.getLoops()) {
            visitLoop(loop);
            loopsToHoist.add(loop);
        }

        for (LoopInformation loop : loopsToHoist) {
            // 2.1. 在循环头前面插一个块，存外提结果
            currentHoistedBlock = new BBlock(function);
            for (var predecessor : new LinkedList<>(function.getCtrlFlowGraph().getPredecessors(loop.getHead()))) {
                if (!loop.getLatchBlocks().contains(predecessor)) {
                    continue;
                }
                Objects.requireNonNull(predecessor.getLastInstruction())
                    .replaceOperand(loop.getHead(), currentHoistedBlock);
                function.getCtrlFlowGraph().insertBetween(loop.getHead(), predecessor, currentHoistedBlock);
                LoopInformation outerLoop = loop.getParentLoop();
                while (outerLoop != null) {
                    outerLoop.insertBefore(loop.getHead(), currentHoistedBlock);
                    outerLoop = outerLoop.getParentLoop();
                }
                for (DoublyLinkedList.Node<Inst> node : loop.getHead().getInstructions()) {
                    Inst inst = node.getValue();
                    if (inst instanceof IPhi phi) {
                        phi.replaceOperand(predecessor, currentHoistedBlock);
                    } else {
                        // Phi一定在块开头处，且连续分布
                        break;
                    }
                }
            }
            // 2.2. 外提
            LinkedHashSet<Inst> marked = new LinkedHashSet<>();
            HashSet<Value> innerDefPoints = new HashSet<>();
            for (BBlock bBlock : loop.getBlocks()) {
                for (DoublyLinkedList.Node<Inst> node : bBlock.getInstructions()) {
                    Inst inst = node.getValue();
                    innerDefPoints.add(inst);
                }
            }
            boolean changed = true;
            while (changed) {
                changed = false;
                for (BBlock bBlock : loop.getBlocks()) {
                    var iter = bBlock.getInstructions().iterator();
                    while (iter.hasNext()) {
                        Inst inst = iter.next().getValue();
                        if (inst instanceof ITerminator || inst instanceof IAllocate || inst instanceof ILoad
                            || inst instanceof IPhi || inst instanceof IStore || inst instanceof ICall) {
                            continue;
                        }
                        boolean allInvariant = true;
                        for (Value operand : inst.getOperands()) {
                            if (innerDefPoints.contains(operand)) {
                                allInvariant = false;
                                break;
                            }
                        }
                        if (allInvariant && !marked.contains(inst)) {
                            marked.add(inst);
                            iter.remove();
                            new DoublyLinkedList.Node<>(inst).insertIntoTail(currentHoistedBlock.getInstructions());
                            innerDefPoints.remove(inst);
                            changed = true;
                        }
                    }
                }
            }

            // 2.3. 把新块插入函数
            new DoublyLinkedList.Node<Inst>(new IBranch(loop.getHead())).insertIntoTail(currentHoistedBlock.getInstructions());
            function.addBBlock(currentHoistedBlock);
        }
    }

    private void visitLoop(LoopInformation loop) {
        for (LoopInformation l : loop.getSubLoops()) {
            visitLoop(l);
            loopsToHoist.add(l);
        }
    }
}
