package backend.mips.instruction;

import backend.mips.MipsBlock;
import backend.mips.operand.Operand;
import backend.mips.operand.PReg;
import backend.mips.operand.VReg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Intermediate inst;
// would be replaced in Phi elimination

public class Phi extends Instruction {
    private Operand res;
    private List<Operand> operands;
    private List<MipsBlock> sources;

    public Phi(Operand res) {
        this.res = res;
        this.operands = new ArrayList<>();
        this.sources = new ArrayList<>();
    }

    public void addOperand(Operand operand, MipsBlock source) {
        operands.add(operand);
        sources.add(source);
    }

    public List<Operand> getOperands() {
        return operands;
    }

    public int getOperandCount() {
        return operands.size();
    }

    public Operand getOperand(int index) {
        return operands.get(index);
    }

    public MipsBlock getSource(int index) {
        return sources.get(index);
    }

    public Operand getRes() {
        return res;
    }

    @Override
    public Set<VReg> getDefVRegs() {
        throw new RuntimeException("Phi shouldn't in PRegAlloc");
    }

    @Override
    public Set<VReg> getUseVRegs() {
        throw new RuntimeException("Phi shouldn't in PRegAlloc");
    }

    @Override
    public void replaceOperand(Operand prevOperand, Operand newOperand) {
        throw new RuntimeException("Phi shouldn't in PRegAlloc");
    }

    @Override
    public void fillPReg(Map<VReg, PReg> colorMap) {
        throw new RuntimeException("Phi shouldn't in PRegAlloc");
    }

    @Override
    public String toMIPS() {
        throw new RuntimeException("Phi shouldn't in final result");
    }
}
