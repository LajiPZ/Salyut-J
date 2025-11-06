package frontend.llvm.value.instruction;

import frontend.llvm.value.Value;
import frontend.datatype.DataType;
import frontend.datatype.PointerType;

public class IAllocate extends Inst {
    public IAllocate(DataType type) {
        super("%" + Value.counter.get(), type);
        assert type instanceof PointerType: "You should use PointerType";
    }

    @Override
    public String toLLVM() {
        return getName() + " = alloca " + ((PointerType) getType()).getBaseType().toString();
    }
}
