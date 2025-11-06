package frontend;

import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.GlobalVariable;
import frontend.llvm.IrModule;
import frontend.llvm.value.Value;
import frontend.llvm.value.instruction.Inst;
import frontend.symbol.VarSymbol;
import frontend.datatype.DataType;
import frontend.syntax.CompileUnit;
import frontend.syntax.declaration.function.FuncFParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public IrBuilder(CompileUnit compileUnit) {
        this.compileUnit = compileUnit;
        this.globalVariableList = new ArrayList<>();
        this.functionMap = new HashMap<>();
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
        BBlock blk = new BBlock();
        func.addBBlock(blk);
        insertPoint = blk;

        // 将形参放进内存；
        // 形参Symbol对应的Value对应变成被分配地址的指针
        // 为什么？考虑函数内要修改这个参数值的情况，我们传入的参数值在调用后是不变的...
        for (FuncFParam fParam : params) {
            Value valIn = fParam.getSymbol().getValue();
            // TODO
        }
        return func;
    }

    public Inst insertInst(Inst inst) {
        return insertInst(insertPoint, inst);
    }

    public Inst insertInst(BBlock targetBBlk, Inst inst) {

    }
}
