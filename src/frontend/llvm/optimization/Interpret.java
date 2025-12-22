package frontend.llvm.optimization;

import frontend.datatype.ArrayType;
import frontend.datatype.DataType;
import frontend.datatype.IntType;
import frontend.datatype.PointerType;
import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.GlobalVariable;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.*;
import utils.DoublyLinkedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class Interpret implements Pass {

    private IrModule top;

    private Function mainFunction = null;
    private Function newMain = new Function("main", new IntType());

    // Interpreter states
    private Function currentFunction = null;
    private DoublyLinkedList.Node<Inst> currentINode = null;
    private BBlock prevBlock = null;
    private Stack<DoublyLinkedList.Node<Inst>> callStack = new Stack<>();

    private HashMap<Value, IntConstant> pointerMap = new HashMap<>(); // pointer -> IntConst
    private HashMap<Value, ArrayList<Value>> allocMap = new HashMap<>(); // pointer -> List<Pointer>; use offset to search List

    private HashMap<Value, Value> valueMap = new HashMap<>();

    @Override
    public void run(IrModule module) {
        top = module;
        for (GlobalVariable gv : module.getGlobalVariableList()) {
            Value pointer = gv.getSymbol().getValue();
            // TODO： 这里至多只支持一维数组
            if (((PointerType) pointer.getType()).getBaseType() instanceof ArrayType dataType) {
                ArrayList<Value> pointerList = new ArrayList<>();
                for (int i = 0; i < gv.getInitList().size(); i++) {
                    Value offsetPointer = new Value(new PointerType(dataType.getFinalDataType()));
                    pointerList.add(offsetPointer);
                    pointerMap.put(offsetPointer, new IntConstant(gv.getInitList().get(i)));
                }
                allocMap.put(pointer, pointerList);
            } else {
                // BaseType
                IntConstant init = new IntConstant(gv.getInitList().get(0));
                pointerMap.put(pointer, init);
            }
        }
        for (Function f : module.getFunctions()) {
            if (f.getName().equals("main")) {
                mainFunction = f;
            }
        }
        currentFunction = mainFunction;
        assert mainFunction != null;
        BBlock currentBlock = mainFunction.getBBlocks().getHead().getValue();
        currentINode = currentBlock.getInstructions().getHead();
        while (!(currentINode.getValue() instanceof IReturn)) {
            currentINode = interpret();
        }
    }

    private DoublyLinkedList.Node<Inst> interpret() {
        Inst currentInst = currentINode.getValue();
        // TODO: 下面的都是值都已知，或允许携带未知值往下尝试的（如ICall）；否则，应该保留这些指令
        if (currentInst instanceof IAllocate alloc) {
            DataType dataType = alloc.getType();
            if (dataType instanceof ArrayType arrayType) {
                ArrayList<Value> allocList = new ArrayList<>();
                for (int i = 0; i < arrayType.getSize(); i++) {
                    Value offsetPointer = new Value(new PointerType(arrayType.getFinalDataType()));
                    allocList.add(offsetPointer);
                    pointerMap.put(offsetPointer, new IntConstant(0));
                }
                allocMap.put(alloc, allocList);
            } else {
                pointerMap.put(alloc, new IntConstant(0));
            }
        }
        else if (currentInst instanceof IBranch branch) {
            // TODO: 怎么处理条件值未知，之后的构建情况？考虑此时currentFunction不是main的情况
            if (branch.isConditinal()) {
                IntConstant cond = (IntConstant) branch.getCond();
                if (cond.getValue() == 1) {
                    return branch.getTrueTarget().getInstructions().getHead();
                } else {
                    return branch.getFalseTarget().getInstructions().getHead();
                }
            } else {
                return branch.getUncondTarget().getInstructions().getHead();
            }
        }
        else if (currentInst instanceof ICalc calc) {
            int l = ((IntConstant)calc.getOperand(0)).getValue();
            int r = ((IntConstant)calc.getOperand(1)).getValue();
            Operator op = calc.getOp();
            valueMap.put(calc, new IntConstant(op.calc(l, r)));
        }
        else if (currentInst instanceof ICall call) {
            Function f = call.getFunction();
            if (top.getFunctions().contains(f)) {
                callStack.push(currentINode);
                currentFunction = f;
                return f.getBBlocks().getHead().getValue().getInstructions().getHead();
            } else {
                // TODO: 保留
            }
        }
        else if (currentInst instanceof IConvert convert) {
            if (convert.isTruncating()) {
                int mask = (1 << convert.getType().getSize() * 8) - 1;
                int prev = ((IntConstant)convert.getOperand(0)).getValue();
                int now = prev & mask;
                valueMap.put(convert, new IntConstant(now));
            } else {
                valueMap.put(convert, convert.getOperand(0));
            }
        }
        else if (currentInst instanceof IGep gep) {
            Value base = gep.getOperand(0);
            int offset = ((IntConstant) gep.getOperand(1)).getValue(); // TODO: 注意，offset可能是变量
            Value offsetPointer = allocMap.get(base).get(offset);
            valueMap.put(gep, offsetPointer);
        }
        else if (currentInst instanceof ILoad load) {
            Value loadPointer = load.getPointer();
            valueMap.put(load, pointerMap.get(loadPointer));
        }
        else if (currentInst instanceof IStore store) {
            Value storePointer = store.getPointer();
            Value storeValue = store.getValue();
            valueMap.put(storePointer, storeValue);
        }
        else if (currentInst instanceof IPhi phi) {
            var sourcePairs = phi.getSourcePairs();
            Value phiResult = null;
            for (var sourcePair : sourcePairs) {
                if (sourcePair.getValue1() == prevBlock) {
                    phiResult = sourcePair.getValue2();
                }
            }
            valueMap.put(phi, phiResult);
        }
        else if (currentInst instanceof IReturn ret) {
            if (currentFunction == mainFunction) {
                return currentINode;
            } else {
                DoublyLinkedList.Node<Inst> callNode = callStack.pop();
                Value callValue = callNode.getValue();
                if (!ret.getOperands().isEmpty()) {
                    Value retVal = ret.getOperands().get(0);
                    valueMap.put(callValue, retVal);
                }
                return callNode.getNext();
            }
        }
        else {
            throw new RuntimeException("Unexpected IR instruction when interpreting: " + currentInst);
        }
        return currentINode.getNext();
    }

    private boolean checkCanInterpret(Inst currentInst) {
        return false;
    }
}

