package frontend.llvm;

import frontend.llvm.value.Function;
import frontend.llvm.value.GlobalVariable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public void printIR(String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        StringBuilder stringBuilder = new StringBuilder();
        for (Function function : externalFunctionMap.values()) {
            stringBuilder.append("declare ").append(function.getType())
                .append(" @").append(function.getName()).append("(")
                .append(
                    function.getParams().stream().map(
                        param -> param.getType().toString()
                    ).collect(Collectors.joining(", "))
                ).append(")\n");
        }
        for (GlobalVariable globalVariable : globalVariableList) {
            stringBuilder.append(globalVariable.toLLVM() + "\n");
        }
        for (Function function : functionMap.values()) {
            stringBuilder.append(function.toString()).append("\n");
        }
        writer.write(stringBuilder.toString());
        writer.close();
    }
}
