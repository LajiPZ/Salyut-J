package frontend.llvm.value.instruction;

import frontend.llvm.value.BBlock;
import frontend.llvm.value.Value;

import java.util.List;
import java.util.Map;

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

    @Override
    public Inst clone() {
        if (getOperands().isEmpty()) {
            return new IReturn();
        } else {
            return new IReturn(
                getOperand(0)
            );
        }
    }

    @Override
    public List<BBlock> getSuccessors() {
        return List.of();
    }


}
