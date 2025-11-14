package frontend.llvm.value;

import frontend.llvm.value.instruction.Inst;

import java.util.ArrayList;

public class BBlock extends Value {
    private Function atFunction;
    private ArrayList<Inst> instructions;

    public BBlock(Function atFunction) {
        super("" + Value.counter.get(), null);
        this.atFunction = atFunction;
        this.instructions = new ArrayList<>();
    }

    public Function getFunction() {
        return atFunction;
    }

    public void addInstruction(Inst inst) {
        instructions.add(inst);
    }

    public Inst getLastInstruction() {
        return instructions.isEmpty() ? null : instructions.get(instructions.size() - 1);
    }

    public ArrayList<Inst> getInstructions() {
        return instructions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t").append(getName()).append(":\n");
        for (Inst inst : instructions) {
            sb.append("\t\t").append(inst.toLLVM()).append("\n");
        }
        return sb.toString();
    }
}
