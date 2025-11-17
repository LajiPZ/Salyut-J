package frontend.llvm.value.instruction;

import frontend.datatype.BaseType;
import frontend.datatype.DataType;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;

public class IConvert extends Inst {

    public IConvert(DataType target, Value val) {
        super("%" + Value.counter.get(), target);
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

    public boolean isTruncating() {
        return getType().getSize() < getOperand(0).getType().getSize();
    }
}
