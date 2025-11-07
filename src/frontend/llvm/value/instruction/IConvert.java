package frontend.llvm.value.instruction;

import frontend.datatype.DataType;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;

public class IConvert extends Inst {

    // TODO
    public IConvert(DataType target, Value val) {
        super("%" + Value.counter.get(), target);
        addOperand(val);
    }
}
