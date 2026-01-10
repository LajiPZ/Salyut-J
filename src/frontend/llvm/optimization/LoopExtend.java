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
            if (loop.getPreHeaderBlocks().size() != 1) throw new RuntimeException("Broke the premise that only one preHeader is present");
            HashMap<Value, Value> replacementMap = new HashMap<>();
            List<BBlock> newBBlocks = new LinkedList<>();
            extended = true;
            try {
                newBBlocks.addAll(extend(
                    loop,
                    replacementMap,
                    function
                ));
            } catch (Exception e) {
                extended = false;
            }
            if (extended) {
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
                            Value replacement = replacementMap.getOrDefault(operand, operand);
                            inst.replaceOperand(i, replacement);
                        }
                    }
                }
            }
            for (BBlock newBBlock : newBBlocks) {
                function.addBBlock(newBBlock);
            }
        }
        return extended;
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

    private List<BBlock> extend(
        LoopInformation loop,
        HashMap<Value, Value> replacementMap,
        Function atFunction
    ) {
        int cnt = 0;

        List<BBlock> returnBBlocks = new LinkedList<>();

        HashMap<Value, Value> knownMap = new HashMap<>(); // 注意！只存储Value -> IntConst
        BBlock now = loop.getHead();
        BBlock prev = loop.getPreHeaderBlocks().get(0);
        BBlock currentNewBBlock = new BBlock(atFunction);
        replacementMap.put(now, currentNewBBlock);
        returnBBlocks.add(currentNewBBlock);
        var currentINode = now.getInstructions().getHead();

        HashMap<Value, Value> knownTemp = new HashMap<>();
        HashMap<Value, Value> replacementTemp = new HashMap<>();
        HashSet<Value> knownRemoveTemp = new HashSet<>();
        HashSet<Value> replacementRemoveTemp = new HashSet<>();

        do {
            if (cnt >= MAX_ITERATIONS) throw new RuntimeException("Max iterations reached");
            Inst inst = currentINode.getValue();
            Inst newInst = inst.clone();

            if (inst instanceof IPhi phi) {
                knownRemoveTemp.add(inst);
                replacementRemoveTemp.add(inst);
                Value valueFromSrc = null;
                for (var sourcePair : phi.getSourcePairs()) {
                    if (sourcePair.getValue1() == prev) {
                        valueFromSrc = sourcePair.getValue2();
                        break;
                    }
                }
                if (valueFromSrc instanceof IntConstant) {
                    knownTemp.put(inst, valueFromSrc);
                } else if (knownMap.containsKey(valueFromSrc)) {
                    knownTemp.put(inst, knownMap.get(valueFromSrc));
                } else {
                    valueFromSrc = replacementMap.getOrDefault(valueFromSrc, valueFromSrc);
                    replacementTemp.put(inst, valueFromSrc);
                }
                currentINode = currentINode.getNext();
                if (!(currentINode.getValue() instanceof IPhi)) {
                    for (Value val : knownRemoveTemp) {
                        knownMap.remove(val);
                    }
                    for (Value val : replacementRemoveTemp) {
                        replacementMap.remove(val);
                    }
                    knownRemoveTemp.clear();
                    replacementRemoveTemp.clear();

                    knownMap.putAll(knownTemp);
                    replacementMap.putAll(replacementTemp);
                    knownTemp.clear();
                    replacementTemp.clear();
                }
            }
            else if (inst instanceof IBranch branch) {
                if (branch.isConditinal()) {
                    IntConstant condVal = eval(branch.getCond(), knownMap);
                    if (condVal.getValue() == 1) { // 条件不能eval的时候，例外会往上Catch住
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
                if (!loop.getExitTargetBlocks().contains(now)) {
                    BBlock nextCurretBBlock = new BBlock(atFunction);
                    replacementMap.put(now, nextCurretBBlock);
                    returnBBlocks.add(nextCurretBBlock);
                    new DoublyLinkedList.Node<Inst>(new IBranch(nextCurretBBlock)).insertIntoTail(currentNewBBlock.getInstructions());
                    currentNewBBlock = nextCurretBBlock;
                    currentINode = now.getInstructions().getHead();
                    cnt++;
                } else {
                    new DoublyLinkedList.Node<Inst>(new IBranch(now)).insertIntoTail(currentNewBBlock.getInstructions());
                }
            }
            else if (inst instanceof ICalc || inst instanceof ICompare || inst instanceof IConvert) {
                knownMap.remove(inst);
                replacementMap.remove(inst);
                IntConstant val = null;
                try { val = eval(inst, knownMap); } catch (Exception ignored) {}
                if (val != null) {
                    knownMap.put(inst, eval(inst, knownMap));
                } else {
                    currentNewBBlock.addInstruction(newInst);
                    replaceOperand(newInst, replacementMap, knownMap);
                    replacementMap.put(inst, newInst);
                }
                currentINode = currentINode.getNext();
            }
            else {
                currentNewBBlock.addInstruction(newInst);
                replaceOperand(newInst, replacementMap, knownMap);
                replacementMap.put(inst, newInst);
                currentINode = currentINode.getNext();
            }
        } while (!loop.getExitTargetBlocks().contains(now));
        replacementMap.putAll(knownMap);
        return returnBBlocks;
    }

    private IntConstant eval(Value value, HashMap<Value, Value> knownValues) {
        if (knownValues.containsKey(value)) return (IntConstant) knownValues.get(value);
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

    private void replaceOperand(Inst inst, HashMap<Value, Value> replacementMap, HashMap<Value, Value> knownValues) {
        HashMap<Value, Value> replacementMapCopy = new HashMap<>(replacementMap);
        replacementMapCopy.putAll(knownValues);
        for (int i = 0; i < inst.getOperands().size(); i++) {
            Value operand = inst.getOperand(i);
            Value replacement = replacementMapCopy.getOrDefault(operand, operand);
            inst.replaceOperand(i, replacement);
        }
    }
}
