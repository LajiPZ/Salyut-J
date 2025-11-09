package frontend.llvm.value.instruction;

import frontend.llvm.value.Value;

public class IReturn extends ITerminator {
    public IReturn() {
        super("", null);
    }

    public IReturn(Value retVal) {
        super("", null);
        addOperand(retVal);
    }

    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append("ret ");
        if (getOperands().isEmpty()) {
            sb.append("void");
        } else {
            sb.append(getOperand(0));
        }
        return sb.toString();
    }
}
