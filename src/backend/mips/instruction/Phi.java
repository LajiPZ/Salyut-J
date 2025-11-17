package backend.mips.instruction;

import backend.mips.MipsBlock;
import backend.mips.operand.Operand;

import java.util.ArrayList;
import java.util.List;

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
}
