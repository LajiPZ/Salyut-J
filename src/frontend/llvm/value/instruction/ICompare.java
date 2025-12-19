package frontend.llvm.value.instruction;

import frontend.datatype.BooleanType;
import frontend.llvm.value.Value;

import java.util.Map;

public class ICompare extends Inst {

    private final Operator op;

    public ICompare(Operator op, Value v1, Value v2) {
        super("%" + Value.counter.get(), new BooleanType());
        addOperand(v1);
        addOperand(v2);
        this.op = op;
    }

    public Operator getOp() {
        return op;
    }

    @Override
    public String toLLVM() {
        return getName() + " = " + op.getOperation() + " " + getOperand(0).getType() + " " +
            getOperand(0).getName() + ", " + getOperand(1).getName();
    }

    @Override
    public Inst clone(Map<Value, Value> replacementMap) {
        return new ICompare(
            op,
            replacementMap.getOrDefault(getOperand(0), getOperand(0)),
            replacementMap.getOrDefault(getOperand(1), getOperand(1))
        );
    }
}
