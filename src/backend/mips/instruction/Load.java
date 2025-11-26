package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.VReg;

import java.util.Set;
import java.util.stream.Collectors;

public class Load extends Mem {

    public Load(Align align, Operand res, Operand base, Operand offset) {
        super(align, res, base, offset);
    }

    @Override
    public Set<VReg> getDefVRegs() {
        return Set.of(src).stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toSet());
    }

    @Override
    public Set<VReg> getUseVRegs() {
        return Set.of(base).stream().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toSet());
    }

    @Override
    public String toMIPS() {
        return "l" + align + "\t" + src.toMIPS() + ", " + offset.toMIPS() + "(" + base.toMIPS() + ")";
    }
}
