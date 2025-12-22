package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.analysis.ControlFlowAnalysis;
import frontend.llvm.analysis.DominatorAnalysis;
import frontend.llvm.analysis.LoopAnalysis;
import frontend.llvm.tools.LoopInformation;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.*;
import utils.DoublyLinkedList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * 循环展开。需要注意的是，前置必须做Mem2Reg，以及必要的分析
 */
public class LoopExtend implements Pass {

    @Override
    public void run(IrModule module) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Function f : module.getFunctions()) {
                Value.counter.set(f.resumeValCounter());
                changed = execute(f);
                f.saveCurrentValCounter(Value.counter.reset());
            }
            new ConstantFolding().run(module);
            new SimplifyControlFlow().run(module);
            new ControlFlowAnalysis().run(module);
            new RemoveUnreachableBBlocks().run(module);
            new DominatorAnalysis().run(module);
            new LoopAnalysis().run(module);
        }
    }

    // 注意，每次应只允许展开最外层循环，避免插入块之后的复杂性
    private boolean execute(Function function) {
        boolean extended = false;
        for (LoopInformation loop : function.getLoops()) {
            visited.clear();
            if (loop.getLatchBlocks().size() != 1) throw new RuntimeException("Broke the premise that only one latch is present");
            if (canExtend(loop)) {
                extended = true;
                List<BBlock> newBBlocks = extend(
                    loop,
                    new HashMap<>(),
                    function,
                    loop.getLatchBlocks().get(0),
                    ((IBranch)loop.getHead().getLastInstruction()).getCond()
                );
                loop.getLatchBlocks().get(0).getLastInstruction().replaceOperand(
                    loop.getHead(),
                    newBBlocks.get(0)
                );
                for (BBlock newBBlock : newBBlocks) {
                    function.addBBlock(newBBlock);
                }
            }
        }
        return extended;
        // return false;
    }

    private boolean canExtend(LoopInformation loop) {
        HashSet<Value> knownValues = new HashSet<>();
        // 我们构造的Loop中，入口一定是LatchBlock，且唯一
        BBlock latch = loop.getLatchBlocks().get(0);
        BBlock head = loop.getHead();
        for (DoublyLinkedList.Node<Inst> inode : head.getInstructions()) {
            Inst inst = inode.getValue();
            if (inst instanceof IPhi phi) {
                // 只有两个sourcePair，对应迭代变量的初始值和更新值
                for (var sourcePair : phi.getSourcePairs()) {
                    if (sourcePair.getValue1() == latch) {
                        if (sourcePair.getValue2() instanceof IntConstant) {
                            knownValues.add(inst);
                            break;
                        } else {
                            return false;
                        }
                    }
                }
            } else if (inst instanceof IBranch branch) {
                // 必定是我们的条件分支指令
                if (!knownValues.contains(branch.getCond())) return false;
            } else {
                // 此处做得很保守，可以得知，此时加载了静态/全局变量就不允许展开，因为涉及内存
                boolean allKnown = true;
                for (Value operand : inst.getOperands()) {
                    if (!knownValues.contains(operand) && !(operand instanceof IntConstant)) {
                        allKnown = false;
                        break;
                    }
                }
                if (allKnown) knownValues.add(inst);
            }
        }
        // 检查迭代变量；事实上是从循环条件入手，递归查使用的Operand是否已知
        return allKnown(((IBranch) head.getLastInstruction()).getCond());
    }

    private HashSet<Value> visited = new HashSet<>();
    private boolean allKnown(Value value) {
        visited.add(value);
        if (value instanceof IntConstant) return true;
        if (value instanceof IPhi phi) {
            boolean allAvail = true;
             for (var sourcePair : phi.getSourcePairs()) {
                 Value val = sourcePair.getValue2();
                 if (!visited.contains(val)) {
                     if (!allKnown(val)) {
                         allAvail = false;
                         break;
                     }
                 }
             }
             return allAvail;
        } else if (value instanceof Inst inst) {
            boolean allAvail = true;
            for (Value operand : inst.getOperands()) {
                if (!allKnown(operand)) {
                    allAvail = false;
                    break;
                }
            }
            return allAvail;
        } else {
            return false;
        }
    }

    private List<BBlock> extend(LoopInformation loop, HashMap<Value, Value> replacementMap, Function atFunction, BBlock currentPrevBlk, Value cond) {
        List<BBlock> newBBlocks = new LinkedList<>();
        // 克隆块
        HashMap<BBlock, BBlock> blockMap = new HashMap<>();
        for (BBlock block : loop.getBlocks()) {
            BBlock newBBlock = new BBlock(atFunction);
            newBBlocks.add(newBBlock);
            replacementMap.put(block, newBBlock);
            blockMap.put(block, newBBlock);
        }
        // 克隆指令
        for (BBlock block : loop.getBlocks()) {
            BBlock newBBlock = (BBlock) replacementMap.get(block);
            for (DoublyLinkedList.Node<Inst> inode : block.getInstructions()) {
                Inst inst = inode.getValue();
                Inst newInst = inst.clone();
                if (inst instanceof IPhi phi) {
                    Value phiVal = null;
                    for (var sourcePair : phi.getSourcePairs()) {
                        BBlock source = sourcePair.getValue1();
                        if (source == currentPrevBlk) {
                            phiVal = sourcePair.getValue2();
                            phiVal = replacementMap.getOrDefault(phiVal, phiVal);
                            phiVal = eval(phiVal);
                            break;
                        }
                    }
                    if (phiVal == null) throw new RuntimeException("PhiVal is null");
                    replacementMap.put(inst, phiVal);
                } else {
                    newBBlock.addInstruction(newInst);
                    replacementMap.put(inst, newInst);
                }
            }
        }
        // 换操作数
        for (BBlock block : newBBlocks) {
            for (DoublyLinkedList.Node<Inst> inode : block.getInstructions()) {
                Inst inst = inode.getValue();
                for (int i = 0; i < inst.getOperands().size(); i++) {
                    Value replacement = replacementMap.getOrDefault(
                        inst.getOperand(i),
                        inst.getOperand(i)
                    );
                    inst.replaceOperand(i, replacement);
                }
            }
        }
        if (eval(replacementMap.get(cond)).getValue() == 1) {
            BBlock newHead = (BBlock) replacementMap.get(loop.getHead());
            BBlock newPrev = findNewPrev(newHead);
            BBlock replacement = newPrev;
            for (var entry : replacementMap.entrySet()) {
                if (entry.getValue() == newPrev) {
                    newPrev = (BBlock) entry.getKey();
                    break;
                }
            }
            List<BBlock> next = extend(
                loop,
                replacementMap,
                atFunction,
                newPrev,
                cond);
            replacement.getLastInstruction().replaceOperand(newHead, next.get(0));
            newBBlocks.addAll(next);
        }
        return newBBlocks;
    }


    private IntConstant eval(Value value) {
        if (value instanceof IntConstant intConstant) {
            return intConstant;
        } else if (value instanceof ICompare compare) {
            int l = eval(compare.getOperand(0)).getValue();
            int r = eval(compare.getOperand(1)).getValue();
            Operator op = compare.getOp();
            return new IntConstant(op.calc(l, r));
        } else if (value instanceof IConvert convert) {
            if (convert.isTruncating()) {
                int mask = (1 << convert.getType().getSize() * 8) - 1;
                int prev = eval(convert.getOperand(0)).getValue();
                int now = prev & mask;
                return new IntConstant(now);
            } else {
                return eval(convert.getOperand(0));
            }
        } else if (value instanceof ICalc calc) {
            int l = eval(calc.getOperand(0)).getValue();
            int r = eval(calc.getOperand(1)).getValue();
            Operator op = calc.getOp();
            return new IntConstant(op.calc(l,r));
        } else {
            throw new RuntimeException("Value: " + value + " cannot be evaluated");
        }
    }

    private BBlock findNewPrev(BBlock current) {
        BBlock now = current;
        BBlock prev = null;
        do {
            prev = now;
            IBranch branch = (IBranch) now.getLastInstruction();
            if (branch.isConditinal()) {
                if (eval(branch.getCond()).getValue() == 1) {
                    now = branch.getTrueTarget();
                } else {
                    now = branch.getFalseTarget();
                }
            } else {
                now = branch.getUncondTarget();
            }
        } while (now != current);
        return prev;
    }
}
