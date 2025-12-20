package frontend.llvm.value.instruction;

import frontend.llvm.value.Value;

import java.util.Map;

public class ICalc extends Inst {
    private final Operator operator;

    public ICalc(Operator operator, Value val1, Value val2) {
        super("%" + Value.counter.get(), val1.getType());
        this.operator = operator;
        addOperand(val1);
        addOperand(val2);
    }

    public Operator getOp() {
        return operator;
    }

    @Override
    public String toLLVM() {
        return getName() + " = " + operator.getOperation() + " " +
            getType() + " " + getOperand(0).getName() + ", " +
            getOperand(1).getName();
    }

    @Override
    public Integer numbering() {
        // 本质是算一个HashCode，省去比较DAG相等的烦恼
        if (operator.swappable()) {
            return (getOperand(0).hashCode() ^ getOperand(1).hashCode()) * operator.ordinal();
        } else {
            return (getOperand(0).hashCode() ^ (7 * getOperand(1).hashCode())) * operator.ordinal() + 1;
        }
    }

    @Override
    public boolean numberingEquals(Inst inst) {
        if (!(inst instanceof ICalc other)) {
            return false;
        }
        if (this.operator != other.operator) {
            return false;
        }
        if (this.operator.swappable()) {
            return (getOperand(0).equals(other.getOperand(0)) && getOperand(1).equals(other.getOperand(1)))
                || (getOperand(0).equals(other.getOperand(1)) && getOperand(1).equals(other.getOperand(0)));
        } else {
            return (getOperand(0).equals(other.getOperand(0)) && getOperand(1).equals(other.getOperand(1)));
        }
    }

    @Override
    public Inst clone() {
        return new ICalc(
            getOp(),
            getOperand(0),
            getOperand(1)
        );
    }


}
