package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.PReg;
import backend.mips.operand.VReg;

import java.util.Map;
import java.util.Set;

public class LoadAddr extends Instruction {
    // TODO: This is a MARS psuedo-inst, can we eliminate it?
    Operand addr;
    Operand dest;

    public LoadAddr(Operand addr, Operand dest) {
        this.addr = addr;
        this.dest = dest;
    }

    @Override
    public Set<VReg> getDefVRegs() {
        return Set.of((VReg) dest);
    }

    @Override
    public Set<VReg> getUseVRegs() {
        return Set.of();
    }

    @Override
    public void replaceOperand(Operand prevOperand, Operand newOperand) {
        if (addr == prevOperand) addr = newOperand;
        if (dest == prevOperand) dest = newOperand;
    }

    @Override
    public void fillPReg(Map<VReg, PReg> colorMap) {
        fillPReg(dest, colorMap);
    }

    @Override
    public String toMIPS() {
        return "la\t" + dest.toMIPS() + ", " + addr.toMIPS();
    }
}
