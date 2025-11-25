package backend.mips;

import backend.mips.process.RemovePhi;
import backend.mips.process.VReg2PReg;
import frontend.llvm.IrModule;
import frontend.llvm.value.Function;
import frontend.llvm.value.GlobalVariable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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

    public void buildFromIR(IrModule module) {
        globalVariables.addAll(
            module.getGlobalVariableList().stream().map(
                MipsGlobalVariable::build
            ).collect(Collectors.toCollection(LinkedList::new))
        );
        for (Function func : module.getFunctions()) {
            MipsFunction mFunc = MipsFunction.build(func, this);
            functions.add(mFunc);
            functionMap.put(func, mFunc);
            if (func.getName().equals("main")) {
                mainFunction = mFunc;
            }
        }
    }

    public void runPostBuildProcessing() {
        runRemovePhi();
        runAllocatePReg();
    }

    private void runRemovePhi() {
        for (MipsFunction func : functions) {
            new RemovePhi(func).run();
        }
    }

    private void runAllocatePReg() {
        for (MipsFunction func : functions) {
            new VReg2PReg(func).run();
        }
    }

    public Map<Function, MipsFunction> getFunctionMap() {
        return functionMap;
    }

    public void addGlobalVariable(MipsGlobalVariable variable) {
        globalVariables.add(variable);
    }

    public void printMIPS(String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n.data\n");

        for (var global : globalVariables) {
            stringBuilder.append("  ").append(global.toMIPS()).append("\n");
        }

        stringBuilder.append("\n.text\n\n");
        stringBuilder.append("# main():\n");
        stringBuilder.append(mainFunction.toMIPS()).append("\n");

        for (MipsFunction func : functions) {
            if (func == mainFunction) continue;
            stringBuilder.append("# ").append(func).append("()\n");
            stringBuilder.append(func.toMIPS()).append("\n");
        }

        writer.write(stringBuilder.toString());
        writer.close();
    }

}
