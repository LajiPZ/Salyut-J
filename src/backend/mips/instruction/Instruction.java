package backend.mips.instruction;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instBuilder.InstBuilder;
import backend.mips.operand.Operand;
import backend.mips.operand.PReg;
import backend.mips.operand.VReg;
import frontend.llvm.value.instruction.Inst;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract public class Instruction {

    public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {
        ArrayList<Instruction> instructions = new ArrayList<>();
        // 1. pre
        instructions.addAll(
            builder.preRun(inst)
        );
        // 2. actual
        instructions.addAll(
            InstBuilder.build(inst, block, builder)
        );
        return instructions;
    }

    abstract public Set<VReg> getDefVRegs();
    abstract public Set<VReg> getUseVRegs();

    abstract public void replaceOperand(Operand prevOperand, Operand newOperand);

    abstract public void fillPReg(Map<VReg, PReg> colorMap);

    protected Operand fillPReg(Operand prev, Map<VReg, PReg> colorMap) {
        if (prev instanceof VReg vReg) {
            if (colorMap.containsKey(vReg)) {
                return colorMap.get(vReg);
            }
        }
        return prev;
    }
}
