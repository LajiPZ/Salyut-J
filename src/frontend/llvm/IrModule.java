package frontend.llvm;

import frontend.llvm.value.Function;
import frontend.llvm.value.GlobalVariable;

import java.util.List;
import java.util.Map;

public class IrModule {
    private List<GlobalVariable> globalVariableList;
    private Map<String, Function> functionMap;
    private Map<String, Function> externalFunctionMap;

    public IrModule(List<GlobalVariable> globalVariableList,
                    Map<String, Function> functionMap,
                    Map<String, Function> externalFunctionMap) {
        this.globalVariableList = globalVariableList;
        this.functionMap = functionMap;
        this.externalFunctionMap = externalFunctionMap;
    }
}
