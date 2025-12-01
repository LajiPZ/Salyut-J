package backend.mips.instruction;

import backend.mips.operand.Operand;
import backend.mips.operand.VReg;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Store extends Mem {

    public Store(Align align, Operand src, Operand base, Operand offset) {
        super(align, src, base, offset);
    }

    @Override
    public Set<VReg> getDefVRegs() {
        return Set.of();
    }

    @Override
    public List<Operand> getDefOperands() {
        return List.of();
    }

    @Override
    public Set<VReg> getUseVRegs() {
        return Stream.of(src, base).distinct().filter(VReg.class::isInstance).map(VReg.class::cast).collect(Collectors.toSet());
    }

    @Override
    public String toMIPS() {
        return "s" + align + "\t" + src.toMIPS() + ", " + offset.toMIPS() + "(" + base.toMIPS() + ")";
    }
}
