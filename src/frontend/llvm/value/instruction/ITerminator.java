package frontend.llvm.value.instruction;

import frontend.datatype.DataType;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Value;

import java.util.List;

abstract public class ITerminator extends Inst {
    public ITerminator(String name, DataType type) {
        super(name, type);
    }

    abstract public List<BBlock> getSuccessors();
}
