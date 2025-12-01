package frontend.llvm.value.instruction;

import frontend.llvm.value.Value;

import java.util.ArrayList;

public class IStore extends Inst {
    public IStore(Value value, Value pointer) {
        super("", null);
        addOperand(value);
        addOperand(pointer);
    }

    public Value getPointer() {
        return getOperand(1);
    }

    public Value getValue() {
        return getOperand(0);
    }

    public String toLLVM() {
        return "store " + getOperand(0) + ", " + getOperand(1);
    }
}
