package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.*;
import utils.DoublyLinkedList;

import java.util.HashMap;
import java.util.List;

/**
 * 对于计算的常量折叠；注意，此处没有处理常量做参数的函数调用
 */
// TODO：常数函数调用
public class ConstantFolding implements Pass {
    @Override
    public void run(IrModule module) {

        for (Function function : module.getFunctions()) {
            execute(function);
        }
    }

    private void execute(Function function) {
        HashMap<Value, IntConstant> map = new HashMap<>();
        boolean changed = true;
        while (changed) {
            changed = false;

            for (var n : function.getBBlocks()) {
                BBlock bBlock = n.getValue();
                for (DoublyLinkedList.Node<Inst> node : bBlock.getInstructions()) {
                    Inst inst = node.getValue();
                    if (!map.containsKey(inst) && (inst instanceof ICalc || inst instanceof ICompare)) {
                        if (inst.getOperand(0) instanceof IntConstant
                        && inst.getOperand(1) instanceof IntConstant) {
                            Operator opcode;
                            if (inst instanceof ICalc) {
                                opcode = ((ICalc) inst).getOp();
                            } else {
                                opcode = ((ICompare) inst).getOp();
                            }
                            int result = opcode.calc(
                                ((IntConstant) inst.getOperand(0)).getValue(),
                                ((IntConstant) inst.getOperand(1)).getValue()
                            );
                            map.put(inst, new IntConstant(result, inst.getType()));
                            changed = true;
                        }
                    }
                    if (!map.containsKey(inst) && inst instanceof IConvert convert) {
                        if (inst.getOperand(0) instanceof IntConstant intConstant) {
                            map.put(
                                inst,
                                new IntConstant(
                                    (int) (intConstant.getValue() & ((1L << (convert.getType().getSize() * 8)) - 1)),
                                    inst.getType()
                                )
                            );
                            changed = true;
                        }
                    }
                    List<Value> operands = inst.getOperands();
                    for (int i = 0; i < operands.size(); i++) {
                        Value operand = operands.get(i);
                        if (map.containsKey(operand)) {
                            inst.replaceOperand(i, map.get(operand));
                            changed = true;
                        }
                    }
                }
            }
        }
    }
}
