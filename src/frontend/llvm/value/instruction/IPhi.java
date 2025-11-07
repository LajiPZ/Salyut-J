package frontend.llvm.value.instruction;

import frontend.datatype.DataType;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Value;

public class IPhi extends Inst {
    public IPhi(DataType type) {
        super("%" + Value.counter.get(), type);
    }

    public void addSourcePair(BBlock source, Value value) {
        addOperand(source);
        addOperand(value);
    }
}
