package frontend.llvm.value.instruction;

import frontend.datatype.PointerType;
import frontend.llvm.value.Value;

public class ILoad extends Inst {
    public ILoad(Value retVal) {
        super("%" + Value.counter.get(), ((PointerType) retVal.getType()).getBaseType());
        addOperand(retVal);
    }

    @Override
    public String toLLVM() {
        return getName() + " = load " + getType() + ", " + getOperand(0);
    }

}
