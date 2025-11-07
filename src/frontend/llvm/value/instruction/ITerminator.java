package frontend.llvm.value.instruction;

import frontend.datatype.DataType;
import frontend.llvm.value.Value;

abstract public class ITerminator extends Inst {
    public ITerminator(String name, DataType type) {
        super(name, type);
    }
}
