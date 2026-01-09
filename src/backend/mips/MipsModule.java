package backend.mips;

import backend.mips.instruction.CP1RegMove;
import backend.mips.instruction.Calc;
import backend.mips.instruction.Instruction;
import backend.mips.operand.AReg;
import backend.mips.operand.CP1Reg;
import backend.mips.operand.Immediate;
import backend.mips.operand.VReg;
import backend.mips.process.ConservativeRemovePhi;
import backend.mips.process.RemovePhi;
import backend.mips.process.VReg2PReg;
import frontend.datatype.BaseType;
import frontend.datatype.PointerType;
import frontend.llvm.IrModule;
import frontend.llvm.value.Function;
import frontend.llvm.value.GlobalVariable;
import frontend.llvm.value.Value;
import settings.Settings;
import utils.DoublyLinkedList;

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
    private HashMap<Value, CP1Reg> globalVarsInCP1Reg = new HashMap<>();
    private HashMap<GlobalVariable, CP1Reg> forCP1Init = new HashMap<>();
    private Map<Function, MipsFunction> functionMap = new HashMap();
    private MipsFunction mainFunction;

    public void buildFromIR(IrModule module) {
        for (GlobalVariable gv : module.getGlobalVariableList()) {
            MipsGlobalVariable mipsGlobalVariable = MipsGlobalVariable.build(gv);
            globalVariables.add(mipsGlobalVariable);
            if (Settings.OptimizeConfig.allowGlobalVarInCP1) {
                Value value = gv.getSymbol().getValue();
                if (((PointerType) value.getType()).getBaseType() instanceof BaseType) {
                    var iterator = CP1Reg.availableCP1Regs.iterator();
                    if (iterator.hasNext()) {
                        CP1Reg cp1Reg = CP1Reg.availableCP1Regs.iterator().next();
                        CP1Reg.availableCP1Regs.remove(cp1Reg);
                        globalVarsInCP1Reg.put(value, cp1Reg);
                        forCP1Init.put(gv, cp1Reg);
                    }
                }
            }
        }

        for (Function func : module.getFunctions()) {
            MipsFunction mFunc = new MipsFunction(func);
            functions.add(mFunc);
            functionMap.put(func, mFunc);
            if (func.getName().equals("main")) {
                mainFunction = mFunc;
            }
        }
        for (MipsFunction function: functions) {
            function.build(this);
        }

        for (var entry : forCP1Init.entrySet()) {
            GlobalVariable gv = entry.getKey();
            VReg initVReg = new VReg();
            var initVal = new DoublyLinkedList.Node<Instruction>(
                new Calc(
                    Calc.Op.addiu,
                    initVReg,
                    AReg.zero,
                    new Immediate(gv.getInitList().get(0))
                )
            );
            initVal.insertIntoHead(mainFunction.getEntry().getInstructions());
            new DoublyLinkedList.Node<Instruction>(
                new CP1RegMove(
                    CP1RegMove.Op.mtc1,
                    initVReg,
                    entry.getValue()
                )
            ).insertAfter(initVal);
        }
    }

    public void runPostBuildProcessing() throws IOException {
        runRemovePhi();
        runAllocatePReg();
    }

    private void runRemovePhi() {
        for (MipsFunction func : functions) {
            if (Settings.OptimizeConfig.useConservativeRemovePhi) {
                new ConservativeRemovePhi(func).run();
            } else {
                new RemovePhi(func).run();
            }
        }
    }

    private void runAllocatePReg() throws IOException {
        if (Settings.DebugConfig.printMIPSBeforePRegAlloc) {
            this.printMIPS(Settings.FilePath.MIPSBeforePRegAlloc);
        }
        for (MipsFunction func : functions) {
            new VReg2PReg(func).run();
        }
    }

    public List<MipsFunction> getFunctions() {
        return functions;
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

    public HashMap<Value, CP1Reg> getGlobalVarsInCP1Reg() {
        return globalVarsInCP1Reg;
    }

}
