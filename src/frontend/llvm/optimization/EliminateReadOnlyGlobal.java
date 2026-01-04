package frontend.llvm.optimization;

import frontend.datatype.ArrayType;
import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.GlobalVariable;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.IGep;
import frontend.llvm.value.instruction.ILoad;
import frontend.llvm.value.instruction.IStore;
import frontend.llvm.value.instruction.Inst;
import utils.DoublyLinkedList;

import java.util.HashMap;
import java.util.List;

public class EliminateReadOnlyGlobal implements Pass {

    @Override
    public void run(IrModule module) {
        HashMap<Value, List<Integer>> readOnly = recognize(module);
        HashMap<Value, Value> replacementMap = new HashMap<>();
        for (Function function : module.getFunctions()) {
            for (var node : function.getBBlocks()) {
                BBlock bBlock = node.getValue();
                replacementMap.putAll(buildReplacementMap(bBlock, readOnly));
            }
        }
        for (Function function : module.getFunctions()) {
            for (var node : function.getBBlocks()) {
                BBlock bBlock = node.getValue();
                updateOperands(bBlock, replacementMap);
            }
        }
    }

    private HashMap<Value, List<Integer>> recognize(IrModule module) {
        HashMap<Value, List<Integer>> readOnly = new HashMap<>();
        for (GlobalVariable gv : module.getGlobalVariableList()) {
            if (!(gv.getSymbol().getDataType() instanceof ArrayType)) {
                readOnly.put(gv.getSymbol().getValue(), gv.getInitList());
            } else if (gv.getSymbol().isConst()) {
                readOnly.put(gv.getSymbol().getValue(), gv.getInitList());
            }
        }
        for (Function function : module.getFunctions()) {
            for (var n : function.getBBlocks()) {
                BBlock bBlock = n.getValue();
                for (DoublyLinkedList.Node<Inst> node : bBlock.getInstructions()) {
                    Inst inst = node.getValue();
                    for (Value operand : inst.getOperands()) {
                        if (inst instanceof IStore) {
                            readOnly.remove(operand);
                        }
                    }
                }
            }
        }
        return readOnly;
    }

    private HashMap<Value, Value> buildReplacementMap(BBlock bBlock, HashMap<Value, List<Integer>> readOnly) {
        HashMap<Value, Value> replacementMap = new HashMap<>();
        for (DoublyLinkedList.Node<Inst> node : bBlock.getInstructions()) {
            Inst inst = node.getValue();
            if (inst instanceof ILoad load) {
                Value pointer = load.getPointer();
                if (readOnly.containsKey(pointer)) {
                    replacementMap.put(inst, new IntConstant(readOnly.get(pointer).get(0), inst.getType()));
                }
            } else if (inst instanceof IGep gep) {
                // 数组
                Value pointer = gep.getOperand(0);
                if (gep.getOperand(1) instanceof IntConstant index && readOnly.containsKey(pointer)) {
                    int idx = index.getValue();
                    if (node.getNext().getValue() instanceof ILoad load && load.getPointer() == gep) {
                        replacementMap.put(load, new IntConstant(readOnly.get(pointer).get(idx), load.getType()));
                    }
                }
            }
        }
        return replacementMap;
    }

    private void updateOperands(BBlock block, HashMap<Value, Value> replacementMap) {
        for (DoublyLinkedList.Node<Inst> node : block.getInstructions()) {
            Inst inst = node.getValue();
            for (int i = 0; i < inst.getOperands().size(); i++) {
                Value operand = inst.getOperands().get(i);
                if (operand instanceof ILoad && replacementMap.containsKey(operand)) {
                    inst.replaceOperand(i, replacementMap.get(operand));
                }
            }
        }
    }
}
