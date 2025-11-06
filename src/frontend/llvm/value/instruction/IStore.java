package frontend.llvm.value.instruction;

import frontend.llvm.value.Value;

import java.util.ArrayList;

public class IStore extends Inst {
    public IStore(Value value, Value pointer) {
        super("", null);
        addOperand(value);
        addOperand(pointer);
    }

    public String toLLVM() {
        return "store " + getOperand(0) + ", " + getOperand(1);
    }
}
