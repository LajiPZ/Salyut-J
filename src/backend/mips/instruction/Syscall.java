package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.PReg;
import backend.mips.operand.VReg;

import java.util.Map;
import java.util.Set;

public class Syscall extends Instruction {

    @Override
    public Set<VReg> getDefVRegs() {
        return Set.of();
    }

    @Override
    public Set<VReg> getUseVRegs() {
        return Set.of();
    }

    @Override
    public void replaceOperand(Operand prevOperand, Operand newOperand) {
        // nope
    }

    @Override
    public void fillPReg(Map<VReg, PReg> colorMap) {
        // nope
    }

    @Override
    public String toMIPS() {
        return "syscall";
    }
}
