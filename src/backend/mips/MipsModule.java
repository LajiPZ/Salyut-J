package backend.mips;

import frontend.llvm.IrModule;
import frontend.llvm.value.Function;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MipsModule {
    private List<MipsFunction> functions = new LinkedList<>();
    private List<MipsGlobalVariable> globalVariables = new LinkedList<>();
    private Map<Function, MipsFunction> functionMap = new HashMap();
    private MipsFunction mainFunction;

    public MipsModule buildFromIR(IrModule module) {
        globalVariables.addAll(
            module.getGlobalVariableList().stream().map(
                MipsGlobalVariable::build
            ).collect(Collectors.toCollection(LinkedList::new))
        );
        for (Function func : module.getFunctions()) {
            MipsFunction mFunc = MipsFunction.build(func, functionMap);
            functions.add(mFunc);
            functionMap.put(func, mFunc);
            if (func.getName().equals("main")) {
                mainFunction = mFunc;
            }
        }
        return this;
    }

}
