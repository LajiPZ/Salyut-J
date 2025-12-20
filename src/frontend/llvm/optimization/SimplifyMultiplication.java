package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.ICalc;
import frontend.llvm.value.instruction.Inst;
import frontend.llvm.value.instruction.Operator;
import utils.DoublyLinkedList;

import java.util.HashMap;

public class SimplifyMultiplication implements Pass {


    @Override
    public void run(IrModule module) {
        for (Function f : module.getFunctions()) {
            Value.counter.set(f.resumeValCounter());
            execute(f);
            f.saveCurrentValCounter(Value.counter.get());
        }
    }

    private void execute(Function func) {
        HashMap<Value, Value> replacementMap = new HashMap<>();
        for (var n : func.getBBlocks()) {
            BBlock block = n.getValue();
            for (DoublyLinkedList.Node<Inst> node : block.getInstructions()) {
                Inst inst = node.getValue();
                if (!(inst instanceof ICalc iCalc)) {
                    continue;
                }
                if (iCalc.getOp() == Operator.MUL) {
                    // 至少有一个是常数，否则什么都做不了
                    int value;
                    Value operand;
                    if (iCalc.getOperand(0) instanceof IntConstant intConstant) {
                        value = intConstant.getValue();
                        operand = iCalc.getOperand(1);
                    } else if (iCalc.getOperand(1) instanceof IntConstant intConstant) {
                        value = intConstant.getValue();
                        operand = iCalc.getOperand(0);
                    } else {
                        continue;
                    }
                    if (value == 0) {
                        replacementMap.put(iCalc, IntConstant.zero);
                    } else if (value == 1) {
                        replacementMap.put(iCalc, operand);
                    } else if (value == -1) {
                        Inst res = new ICalc(Operator.SUB, IntConstant.zero, operand);
                        new DoublyLinkedList.Node<>(res).insertBefore(node);
                        replacementMap.put(iCalc, res);
                    } else if (value > 0) {
                        // 用移位来换...
                        if (Integer.bitCount(value) == 1) {
                            // 乘的一定是2^n
                            int shift = Integer.numberOfTrailingZeros(value);
                            Inst res = new ICalc(Operator.SLL, operand, new IntConstant(shift));
                            new DoublyLinkedList.Node<>(res).insertBefore(node);
                            replacementMap.put(iCalc, res);
                        } else if (Integer.bitCount(value) == 2) {
                            // e.g. a * 6 = a * 4 + a * 2
                            int shift1 = Integer.numberOfTrailingZeros(value);
                            int shift2 = Integer.numberOfTrailingZeros(value >> (shift1 + 1)) + shift1 + 1;
                            Inst res1 = new ICalc(Operator.SLL, operand, new IntConstant(shift1));
                            new DoublyLinkedList.Node<>(res1).insertBefore(node);
                            Inst res2 = new ICalc(Operator.SLL, operand, new IntConstant(shift2));
                            new DoublyLinkedList.Node<>(res2).insertBefore(node);
                            Inst res = new ICalc(Operator.ADD, res1, res2);
                            new DoublyLinkedList.Node<>(res).insertBefore(node);
                            replacementMap.put(iCalc, res);
                        } else {
                            // e.g. a * 12 = a * 16 - a * 4
                            int subShift = checkAvail(value);
                            if (subShift == -1) return;
                            int addShift = Integer.numberOfTrailingZeros(value + (1 << subShift));
                            Inst res1 = new ICalc(Operator.SLL, operand, new IntConstant(subShift));
                            new DoublyLinkedList.Node<>(res1).insertBefore(node);
                            Inst res2 = new ICalc(Operator.SLL, operand, new IntConstant(addShift));
                            new DoublyLinkedList.Node<>(res2).insertBefore(node);
                            Inst res = new ICalc(Operator.SUB, res2, res1);
                            new DoublyLinkedList.Node<>(res).insertBefore(node);
                            replacementMap.put(iCalc, res);
                        }
                    }
                }
            }
        }
        for (var n : func.getBBlocks()) {
            BBlock block = n.getValue();
            for (DoublyLinkedList.Node<Inst> node : block.getInstructions()) {
                Inst inst = node.getValue();
                replacementMap.forEach(inst::replaceOperand);
            }
        }
    }

    private static int checkAvail(int value) {
        for (int i = 0; i < 32; i++) {
            if (Integer.bitCount(value + (1 << i)) == 1) {
                return i;
            }
        }
        return -1;
    }
}
