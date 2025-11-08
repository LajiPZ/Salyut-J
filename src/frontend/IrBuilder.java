package frontend;

import frontend.datatype.PointerType;
import frontend.llvm.tools.LoopInfo;
import frontend.llvm.value.*;
import frontend.llvm.IrModule;
import frontend.llvm.tools.ValueConverter;
import frontend.llvm.value.instruction.*;
import frontend.symbol.ValSymbol;
import frontend.symbol.VarSymbol;
import frontend.datatype.DataType;
import frontend.syntax.CompileUnit;
import frontend.syntax.declaration.function.FuncFParam;
import frontend.syntax.misc.LVal;

import java.util.*;

/**
 * IrBuilder is used to build the LLVM IR top, that is, IrModule,
 * from SysY source code.
 * After IrModule is built, optimization on IR is done therein,
 * instead of here.
 */

public class IrBuilder {
    private static Map<String, Function> externalFunctionMap = Map.ofEntries(

    );

    private CompileUnit compileUnit;
    private List<GlobalVariable> globalVariableList;
    private Map<String, Function> functionMap;

    private BBlock insertPoint;
    private Function currentFunction;

    private Stack<LoopInfo> loopStack;

    public IrBuilder(CompileUnit compileUnit) {
        this.compileUnit = compileUnit;
        this.globalVariableList = new ArrayList<>();
        this.functionMap = new HashMap<>();
        this.loopStack = new Stack<>();
    }

    public IrModule build() {
        compileUnit.build(this);
        return new IrModule(globalVariableList, functionMap, externalFunctionMap);
    }

    public Function registerFunction(String name, DataType dataType, List<FuncFParam> params) {
        // 1. 构建LLVM视角的函数
        Function func = new Function(name, dataType);
        functionMap.put(name, func);
        currentFunction = func;

        for (FuncFParam fParam : params) {
            // 给每一个fParam都分配Value，绑到它对应的Symbol上
            VarSymbol symbol = fParam.getSymbol();
            symbol.setValue(
                func.addParam(symbol.getDataType())
            );
        }

        // 2. 创建初始块，处理参数
        BBlock blk = new BBlock(func);
        func.addBBlock(blk);
        insertPoint = blk;

        // 将形参放进内存；
        // 形参Symbol对应的Value对应变成被分配地址的指针
        // 为什么？考虑函数内要修改这个参数值的情况，我们传入的参数值在调用后是不变的...
        for (FuncFParam fParam : params) {
            Value valIn = fParam.getSymbol().getValue();
            Value pointer = insertInst(
                    new IAllocate(
                        new PointerType(fParam.getSymbol().getDataType())
                    )
            );
            insertInst(
                    new IStore(valIn, pointer)
            );

            fParam.getSymbol().setValue(pointer);
            fParam.getSymbol().setFromParam();
        }

        return func;
    }

    public BBlock newBBlock(boolean fallThrough) {
        BBlock bBlock = new BBlock(currentFunction);
        currentFunction.addBBlock(bBlock);
        // TODO: 为什么？
        if (fallThrough && !(insertPoint.getLastInstruction() instanceof ITerminator)) {
            insertInst(
                    new IBranch(bBlock)
            );
        }
        insertPoint = bBlock;
        return bBlock;
    }

    public BBlock getInsertPoint() {
        return insertPoint;
    }

    public Inst insertInst(Inst inst) {
        return insertInst(insertPoint, inst);
    }

    public Inst insertInst(BBlock targetBBlk, Inst inst) {
        // TODO: 处理已有的Terminator？
        targetBBlk.addInstruction(inst);
        return inst;
    }

    public Function getFunction(String name) {
        // 先找外部函数，再找内部函数
        if (externalFunctionMap.containsKey(name)) {
            return externalFunctionMap.get(name);
        } else {
            return functionMap.get(name);
        }
    }

    public Value callGep(ValSymbol val, List<Value> idxList) {
        Value current = val.isFromParam() ?
                insertInst(new ILoad(val.getValue())) :
                val.getValue();
        Value retPtr = insertInst(
                new IGep(current, idxList.get(0), val.isFromParam())
        );
        // 多维数组
        for (int i = 1; i < idxList.size(); i++) {
            retPtr = insertInst(
                new IGep(retPtr, idxList.get(i), val.isFromParam())
            );
        }
        return retPtr;
    }

    public void doAssign(LVal left, Value right) {
        Value pointer;
        if (left.getIndexList().isEmpty()) {
            pointer = left.getValSymbol().getValue();
        } else {
            pointer = callGep(left.getValSymbol(), left.getIndexList().stream().map(exp ->
                exp.build(this)
            ).toList());
        }
        right = ValueConverter.toBaseType(pointer, right);
        insertInst(
            new IStore(right, pointer)
        );
    }

    public void fillBranchTarget(BBlock falseBBlk, List<BBlock> trueBBlks, IrBuilder builder) {
        BBlock mergeBlk = builder.newBBlock(false);
        builder.insertInst(falseBBlk, new IBranch(mergeBlk));
        for (BBlock trueBBlk : trueBBlks) {
            if (trueBBlk.getLastInstruction() instanceof IBranch branch) {
                branch.fillNullTarget(mergeBlk);
            }
        }
    }

    public Function getCurrentFunction() {
        return currentFunction;
    }

    public void intoLoop(BBlock condBBlk) {
        loopStack.add(new LoopInfo(condBBlk));
    }

    public LoopInfo getCurrentLoop() {
        return loopStack.lastElement();
    }

    public void exitLoop() {
        loopStack.pop();
    }
}
