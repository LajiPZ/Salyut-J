package frontend.llvm.value.instruction;

import frontend.datatype.BooleanType;
import frontend.llvm.value.Value;

public class ICompare extends Inst {

    private final Operator op;

    public ICompare(Operator op, Value v1, Value v2) {
        super("%" + Value.counter.get(), new BooleanType());
        addOperand(v1);
        addOperand(v2);
        this.op = op;
    }

    @Override
    public String toLLVM() {
        return getName() + " = " + op.getOperation() + " " + getOperand(0).getType() +
            getOperand(0).getName() + ", " + getOperand(1).getName();
    }
}
