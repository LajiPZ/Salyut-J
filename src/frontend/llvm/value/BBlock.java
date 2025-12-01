package frontend.llvm.value;

import backend.mips.instruction.Instruction;
import frontend.llvm.value.instruction.ITerminator;
import frontend.llvm.value.instruction.Inst;
import utils.DoublyLinkedList;

import java.util.ArrayList;
import java.util.List;

public class BBlock extends Value {
    private Function atFunction;
    private DoublyLinkedList<Inst> instructions;

    public BBlock(Function atFunction) {
        super("" + Value.counter.get(), null);
        this.atFunction = atFunction;
        this.instructions = new DoublyLinkedList<>();
    }

    public Function getFunction() {
        return atFunction;
    }

    public void addInstruction(Inst inst) {
        new DoublyLinkedList.Node<>(inst).insertIntoTail(instructions);
    }

    public Inst getLastInstruction() {
        DoublyLinkedList.Node<Inst> node =  instructions.getTail();
        if (node == null) return null;
        return node.getValue();
    }

    public DoublyLinkedList<Inst> getInstructions() {
        return instructions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t").append(getName()).append(":\n");
        for (DoublyLinkedList.Node<Inst> node : instructions) {
            Inst inst = node.getValue();
            sb.append("\t\t").append(inst.toLLVM()).append("\n");
        }
        return sb.toString();
    }

    public List<BBlock> getSuccessors() {
        Inst last = getLastInstruction();
        if (!(last instanceof ITerminator terminator)) {
            throw new RuntimeException("last Inst in a BBlock should be an ITerminator");
        }
        return terminator.getSuccessors();
    }
}
