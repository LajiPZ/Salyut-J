package frontend.llvm.value.instruction;

import frontend.datatype.BaseType;
import frontend.datatype.DataType;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;

import java.util.Map;

public class IConvert extends Inst {

    private DataType target;

    public IConvert(DataType target, Value val) {
        super("%" + Value.counter.get(), target);
        this.target = target;
        addOperand(val);
    }

    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(" = ");
        if (getType().getSize() < getOperand(0).getType().getSize()) {
            sb.append("trunc ");
        } else {
            sb.append("zext ");
        }
        sb.append(getOperand(0)).append(" to ").append(getType());
        return sb.toString();
    }

    @Override
    public Inst clone(Map<Value, Value> replacementMap) {
        return new IConvert(
            target,
            replacementMap.getOrDefault(getOperand(0), getOperand(0))
        );
    }

    public boolean isTruncating() {
        return getType().getSize() < getOperand(0).getType().getSize();
    }


}
