package frontend.llvm.value.instruction;

import frontend.llvm.tools.UseRecord;
import frontend.llvm.value.Value;
import frontend.datatype.DataType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;


/**
 * 按照教程中的LLVM IR结构，应该还有User，Inst继承User类
 * 考虑到Inst是User的唯一子类，此处选择跳过User，直接继承自Value
 */
abstract public class Inst extends Value {

    protected List<Value> operands;
    private List<UseRecord> uses = new LinkedList<>();

    public Inst(String name, DataType type) {
        super(name, type);
        this.operands = new ArrayList<>();
    }

    public void addOperand(Value operand) {
        this.operands.add(operand);
        if (operand != null) this.addUse(new UseRecord(operand, this, operands.size() - 1));
    }

    public Value getOperand(int index) {
        return this.operands.get(index);
    }

    public void replaceOperand(int index, Value operand) {
        Value old = this.operands.get(index);
        this.operands.set(index, operand);
        if (old != null) this.removeOneUse(old);
        this.addUse(new UseRecord(operand, this, index));
    }

    public void replaceOperand(Value prev, Value updated) {
        IntStream.range(0, this.operands.size())
            .filter(i -> this.operands.get(i).equals(prev))
            .forEach(i -> replaceOperand(i, updated));
    }

    public void dropOperand(int index) {
        Value old = this.operands.get(index);
        this.operands.remove(index);
        this.removeOneUse(old);
    }

    public List<Value> getOperands() {
        return this.operands;
    }

    abstract public String toLLVM();

    public Integer numbering() {
        return null;
    }

    public boolean numberingEquals(Inst other) {
        return false;
    }

    private void addUse(UseRecord use) {
        this.uses.add(use);
    }

    public List<UseRecord> getUses() {
        return this.uses;
    }

    public void removeOneUse(Value inst) {
        IntStream.range(0, this.uses.size())
            .filter(i -> this.uses.get(i).getValue().equals(inst))
            .findFirst().ifPresent(uses::remove);
    }

    @Override
    abstract public Inst clone();
}
