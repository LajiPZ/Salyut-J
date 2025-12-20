package frontend.llvm.value.instruction;

import frontend.llvm.value.BBlock;
import frontend.llvm.value.Value;

import java.util.List;
import java.util.Map;

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

    public BBlock getUncondTarget() {
        return (BBlock) getOperand(0);
    }

    public Value getCond() {
        return getOperand(0);
    }

    public BBlock getTrueTarget() {
        return (BBlock) getOperand(1);
    }

    public BBlock getFalseTarget() {
        return (BBlock) getOperand(2);
    }

    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append("br ");
        if (isConditinal()) {
            sb.append(getOperand(0)).append(", ");
            sb.append("label %").append(getOperand(1).getName()).append(", ");
            sb.append("label %").append(getOperand(2).getName());
        } else {
            sb.append("label %").append(getOperand(0).getName());
        }
        return sb.toString();
    }

    @Override
    public Inst clone() {
        if (isConditinal()) {
            return new IBranch(
                this.getCond(),
                this.getTrueTarget(),
                this.getFalseTarget()
            );
        } else {
            return new IBranch(
                getUncondTarget()
            );
        }
    }

    @Override
    public List<BBlock> getSuccessors() {
        if (isConditinal()) {
            return List.of((BBlock) getOperand(1), (BBlock) getOperand(2));
        } else {
            return List.of((BBlock) getOperand(0));
        }
    }


}
