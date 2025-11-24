package backend.mips.instruction;

import backend.mips.operand.VReg;

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
}
