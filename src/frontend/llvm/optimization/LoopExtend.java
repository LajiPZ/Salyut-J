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
import settings.Settings;
import utils.DoublyLinkedList;

import java.util.*;

/**
 * 循环展开。需要注意的是，前置必须做Mem2Reg，以及必要的分析
 */
public class LoopExtend implements Pass {

    private static int MAX_ITERATIONS = Settings.OptimizeConfig.maxLoopExtendIterations;
    private List<LoopInformation> loopsToExtend = new LinkedList<>();

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
            new EliminateDeadCode().run(module);
        }
    }

    // 从最内层展开
    private boolean execute(Function function) {
        loopsToExtend.clear();
        boolean extended = false;
        // 找出所有的最内层循环；这些内层循环一定不相交
        for (LoopInformation loop : function.getLoops()) {
            visitLoop(loop);
        }
        for (LoopInformation loop : loopsToExtend) {
            visited.clear();
            if (loop.getPreHeaderBlocks().size() != 1) throw new RuntimeException("Broke the premise that only one preHeader is present");
            if (canExtend(loop)) {
                HashMap<Value, Value> test = new HashMap();
                extended = true;
                List<BBlock> newBBlocks = extend(
                    loop,
                    test,
                    function,
                    loop.getPreHeaderBlocks().get(0),
                    ((IBranch)loop.getHead().getLastInstruction()).getCond(),
                    -1,
                    MAX_ITERATIONS
                );
                loop.getPreHeaderBlocks().get(0).getLastInstruction().replaceOperand(
                    loop.getHead(),
                    newBBlocks.get(0)
                );
                // 尝试更换
                for (var bNode : function.getBBlocks()) {
                    BBlock block = bNode.getValue();
                    for (var iNode : block.getInstructions()) {
                        Inst inst = iNode.getValue();
                        for (int i = 0; i < inst.getOperands().size(); i++) {
                            Value operand = inst.getOperand(i);
                            Value replacement = test.getOrDefault(operand, operand);
                            inst.replaceOperand(i, replacement);
                        }
                    }
                }
                for (BBlock newBBlock : newBBlocks) {
                    function.addBBlock(newBBlock);
                }

            }
        }
        return extended;
        // return false;
    }


    private void visitLoop(LoopInformation loop) {
        if (loop.getSubLoops().isEmpty()) {
            loopsToExtend.add(loop);
        } else {
            for (LoopInformation subLoop : loop.getSubLoops()) {
                visitLoop(subLoop);
            }
        }
    }

    private boolean canExtend(LoopInformation loop) {
        if (loop.getExitBlocks().size() != 1) return false;
        HashSet<Value> knownValues = new HashSet<>();
        // 我们构造的Loop中，入口一定是LatchBlock，且唯一
        BBlock preHeader = loop.getPreHeaderBlocks().get(0);
        BBlock head = loop.getHead();
        for (DoublyLinkedList.Node<Inst> inode : head.getInstructions()) {
            Inst inst = inode.getValue();
            if (inst instanceof IPhi phi) {
                // 只有两个sourcePair，对应迭代变量的初始值和更新值
                for (var sourcePair : phi.getSourcePairs()) {
                    if (sourcePair.getValue1() == preHeader) {
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
            } else if (inst instanceof ICall call) {
                if (call.getFunction().getName().equals("getint")) return false;
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

        boolean avail = true;
        try {
            HashMap<Value, IntConstant> knownMap = new HashMap<>();
            BBlock now = loop.getHead();
            BBlock prev = loop.getPreHeaderBlocks().get(0);
            var currentINode = now.getInstructions().getHead();
            do {
                Inst inst = currentINode.getValue();
                if (inst instanceof IPhi phi) {
                    Value valueFromSrc = null;
                    for (var sourcePair : phi.getSourcePairs()) {
                        if (sourcePair.getValue1() == prev) {
                            valueFromSrc = sourcePair.getValue2();
                            break;
                        }
                    }
                    if (valueFromSrc instanceof IntConstant) {
                        knownMap.put(inst, (IntConstant) valueFromSrc);
                    } else if (knownMap.containsKey(valueFromSrc)) {
                        knownMap.put(inst, knownMap.get(valueFromSrc));
                    } else {
                        return false;
                    }
                    currentINode = currentINode.getNext();
                } else if (inst instanceof IBranch branch) {
                    if (branch.isConditinal()) {
                        if (eval(branch.getCond(), knownMap).getValue() == 1) {
                            prev = now;
                            now = branch.getTrueTarget();
                        } else {
                            prev = now;
                            now = branch.getFalseTarget();
                        }
                    } else {
                        prev = now;
                        now = branch.getUncondTarget();
                    }
                    currentINode = now.getInstructions().getHead();
                } else if (
                    inst instanceof ICalc ||
                    inst instanceof ICompare ||
                    inst instanceof IConvert
                ) {
                    knownMap.remove(inst);
                    knownMap.put(inst, eval(inst, knownMap));
                    currentINode = currentINode.getNext();
                } else {
                    currentINode = currentINode.getNext();
                }
            } while (!loop.getExitTargetBlocks().contains(now));
        } catch (Exception e) {
            avail = false;
        }

        // TODO：你需要修改allKnown的逻辑；他是有问题的，正确做法是往下一直走，
        return allKnown(((IBranch) head.getLastInstruction()).getCond()) && avail;
    }

    private IntConstant eval(Value value, HashMap<Value, IntConstant> knownValues) {
        if (knownValues.containsKey(value)) return knownValues.get(value);
        if (value instanceof IntConstant intConstant) {
            return intConstant;
        } else if (value instanceof ICompare compare) {
            int l = eval(compare.getOperand(0), knownValues).getValue();
            int r = eval(compare.getOperand(1), knownValues).getValue();
            Operator op = compare.getOp();
            return new IntConstant(op.calc(l, r));
        } else if (value instanceof IConvert convert) {
            if (convert.isTruncating()) {
                int mask = (1 << convert.getType().getSize() * 8) - 1;
                int prev = eval(convert.getOperand(0), knownValues).getValue();
                int now = prev & mask;
                return new IntConstant(now);
            } else {
                return eval(convert.getOperand(0), knownValues);
            }
        } else if (value instanceof ICalc calc) {
            int l = eval(calc.getOperand(0), knownValues).getValue();
            int r = eval(calc.getOperand(1), knownValues).getValue();
            Operator op = calc.getOp();
            return new IntConstant(op.calc(l,r));
        } else {
            throw new RuntimeException("Value: " + value + " cannot be evaluated");
        }
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

    private List<BBlock> extend(
        LoopInformation loop,
        HashMap<Value, Value> replacementMap,
        Function atFunction,
        BBlock currentPrevBlk,
        Value cond, int prevCondVal,
        int remainingIterations
    ) {
        HashMap<Value, Value> psuedoReplacementMap = new HashMap();
        HashMap<Value, Value> newToOld = new HashMap<>();
        List<BBlock> newBBlocks = new LinkedList<>();
        List<BBlock> returnNewBBlocks = new LinkedList<>();
        if (remainingIterations < 0) {
            return newBBlocks;
        }
        // 克隆块
        for (BBlock block : loop.getBlocks()) {
            BBlock newBBlock = new BBlock(atFunction);
            newBBlocks.add(newBBlock);
            psuedoReplacementMap.put(block, newBBlock);
            newToOld.put(newBBlock, block);
        }
        // 克隆指令
        for (BBlock block : loop.getBlocks()) {
            BBlock newBBlock = (BBlock) psuedoReplacementMap.get(block);
            for (DoublyLinkedList.Node<Inst> inode : block.getInstructions()) {
                Inst inst = inode.getValue();
                Inst newInst = inst.clone();
                if (newInst instanceof IPhi phi) {
                    for (int i = 0; i < newInst.getOperands().size(); i++) {
                        Value replacement = replacementMap.getOrDefault(
                            newInst.getOperand(i),
                            newInst.getOperand(i)
                        );
                        newInst.replaceOperand(i, replacement);
                    }
                    /*
                    // 此时来源一定是currentPrevBlk，故其他的sourcePair需要丢弃，防止控制流合并时出错
                    if (phi.getOperands().contains(currentPrevBlk)) {
                        LinkedList<Integer> droppedIndex = new LinkedList<>();
                        for (int i = 0; i < phi.getSourcePairs().size(); i++) {
                            if (currentPrevBlk != phi.getSourcePairs().get(i).getValue1()) {
                                droppedIndex.add(i);
                            }
                        }
                        for (int i = droppedIndex.size() - 1; i >= 0; i--) {
                            phi.dropSourcePair(droppedIndex.get(i));
                        }
                    }
                     */

                    LinkedList<Integer> droppedIndex = new LinkedList<>();
                    for (int i = 0; i < phi.getSourcePairs().size(); i++) {
                        if (!newBBlocks.contains(phi.getSourcePairs().get(i).getValue1())) {
                            droppedIndex.add(i);
                        }
                    }
                    for (int i = droppedIndex.size() - 1; i >= 0; i--) {
                        phi.dropSourcePair(droppedIndex.get(i));
                    }

                    Value phiVal = null;
                    for (var sourcePair : phi.getSourcePairs()) {
                        BBlock source = sourcePair.getValue1();
                        if (source == currentPrevBlk) {
                            phiVal = sourcePair.getValue2();
                            phiVal = psuedoReplacementMap.getOrDefault(phiVal, phiVal);
                            try {
                                phiVal = eval(phiVal);
                            } catch (Exception e) {
                                phiVal = null;
                            }
                            break;
                        }
                    }
                    if (phiVal == null) {
                        newBBlock.addInstruction(newInst);
                        psuedoReplacementMap.put(inst, newInst);
                        newToOld.put(newInst, inst);
                    } else {
                        psuedoReplacementMap.put(inst, phiVal);
                        newToOld.put(phiVal, inst);
                    }
                } else {
                    newBBlock.addInstruction(newInst);
                    psuedoReplacementMap.put(inst, newInst);
                    newToOld.put(newInst, inst);
                }
            }
        }
        // 换操作数
        for (BBlock block : newBBlocks) {
            for (DoublyLinkedList.Node<Inst> inode : block.getInstructions()) {
                Inst inst = inode.getValue();
                for (int i = 0; i < inst.getOperands().size(); i++) {
                    Value replacement = psuedoReplacementMap.getOrDefault(
                        inst.getOperand(i),
                        inst.getOperand(i)
                    );
                    inst.replaceOperand(i, replacement);
                }
            }
        }
        int condVal = eval(psuedoReplacementMap.get(cond)).getValue();
        if (prevCondVal == -1 || condVal == prevCondVal) {
            replacementMap.putAll(psuedoReplacementMap);
            BBlock loopHead = loop.getHead();
            if (loop.getLatchBlocks().size() != 1) {
                throw new RuntimeException("Nope");
            }
            // 其实是在找Latch
            BBlock latch = loop.getLatchBlocks().get(0);
            BBlock prevForNextUnfold = (BBlock) replacementMap.get(latch);
            List<BBlock> next = extend(
                loop,
                replacementMap,
                atFunction,
                prevForNextUnfold,
                cond,
                condVal,
                remainingIterations - 1);
            BBlock newHead = (BBlock) replacementMap.get(loop.getHead());
            if (!next.isEmpty() && prevForNextUnfold != null) prevForNextUnfold.getLastInstruction().replaceOperand(newBBlocks.get(0), next.get(0)); // 这是对上面复制好的指令换的
            newBBlocks.addAll(next);
            returnNewBBlocks.addAll(newBBlocks);
        } else {
            // 好，跳变了
            BBlock now = newBBlocks.get(0);
            do {
                BBlock oldBlk = (BBlock) newToOld.get(now);
                replacementMap.put(oldBlk, now);
                for (var iNode : oldBlk.getInstructions()) {
                    Value oldInst = iNode.getValue();
                    Value newInstValue = psuedoReplacementMap.get(oldInst);
                    replacementMap.put(oldInst, newInstValue);
                }
                returnNewBBlocks.add(now);
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
            } while (!loop.getExitTargetBlocks().contains(now));

        }
        return returnNewBBlocks;
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
}
