package frontend.llvm.value.instruction;

import frontend.llvm.value.Value;

public class ICalc extends Inst {
    private final Operator operator;

    public ICalc(Operator operator, Value val1, Value val2) {
        super("%" + Value.counter.get(), val1.getType());
        this.operator = operator;
        addOperand(val1);
        addOperand(val2);
    }

    @Override
    public String toLLVM() {
        return ""; // TODO
    }
}
