package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;
import frontend.llvm.value.instruction.Inst;
import utils.DoublyLinkedList;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class LocalVariableNumbering implements Pass {

    @Override
    public void run(IrModule module) {
        for (Function f : module.getFunctions()) {
            for (BBlock block : f.getBBlocks()) {
                execute(block);
            }
        }
    }

    private void execute(BBlock bblock) {
        HashMap<Integer, List<Inst>> map = new HashMap<>();
        HashMap<Value, Value> replaceMap = new HashMap<>();
        for (DoublyLinkedList.Node<Inst> node : bblock.getInstructions()) {
            Inst inst = node.getValue();
            Integer numbering = inst.numbering();
            if (numbering != null) {
                if (map.containsKey(numbering)) {
                    for (var other : map.get(numbering)) {
                        // map理应只有一个；如果不是，那么就是hash碰撞了，这就是numberingEquals再检查一遍的意义
                        if (inst.numberingEquals(other)) {
                            replaceMap.put(inst, other);
                            break;
                        }
                    }
                }
                map.computeIfAbsent(numbering, k -> new LinkedList<>()).add(inst);
            }
            for (int i = 0; i < inst.getOperands().size(); i++) {
                Value operand = inst.getOperands().get(i);
                if (replaceMap.containsKey(operand)) {
                    inst.replaceOperand(i, replaceMap.get(operand));
                }
            }
        }
    }
}
