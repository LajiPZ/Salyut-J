package frontend.llvm.value.instruction;

import frontend.llvm.value.BBlock;
import frontend.llvm.value.Value;

public class IBranch extends ITerminator {

    public IBranch(BBlock target) {
        super("", null);
        addOperand(target);
    }

    public IBranch(Value cond, BBlock trueTarget, BBlock falseTarget) {
        super("",null);
        addOperand(cond);
        addOperand(trueTarget);
        addOperand(falseTarget);
    }

    public boolean isConditinal() {
        return getOperands().size() == 3;
    }

    public void fillNullTarget(BBlock target) {
        if (isConditinal()) {
            if (getOperand(1) == null) {
                replaceOperand(1, target);
            }
            if (getOperand(2) == null) {
                replaceOperand(2, target);
            }
        } else {
            if (getOperand(0) == null) {
                replaceOperand(0, target);
            }
        }
    }

    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append("br ");
        if (isConditinal()) {
            sb.append(getOperand(0).getName()).append(", ");
            sb.append("label %").append(getOperand(1).getName()).append(", ");
            sb.append("label %").append(getOperand(2).getName());
        } else {
            sb.append("label %").append(getOperand(0).getName());
        }
        return sb.toString();
    }
}
