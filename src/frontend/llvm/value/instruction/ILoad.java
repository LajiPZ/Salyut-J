package frontend.llvm.value.instruction;

import frontend.datatype.PointerType;
import frontend.llvm.value.Value;

import java.util.Map;

public class ILoad extends Inst {
    public ILoad(Value retVal) {
        super("%" + Value.counter.get(), ((PointerType) retVal.getType()).getBaseType());
        addOperand(retVal);
    }

    public Value getPointer() {
        return getOperand(0);
    }

    @Override
    public String toLLVM() {
        return getName() + " = load " + getType() + ", " + getOperand(0);
    }

    @Override
    public Inst clone(Map<Value, Value> replacementMap) {
        return new ILoad(replacementMap.getOrDefault(getOperand(0), getOperand(0)));
    }


}
