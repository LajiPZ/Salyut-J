package frontend.llvm.value.instruction;

import frontend.llvm.value.Value;
import frontend.datatype.DataType;
import frontend.datatype.PointerType;

import java.util.Map;

public class IAllocate extends Inst {
    public IAllocate(DataType type) {
        super("%" + Value.counter.get(), type);
        assert type instanceof PointerType: "You should use PointerType";
    }

    @Override
    public String toLLVM() {
        return getName() + " = alloca " + ((PointerType) getType()).getBaseType().toString();
    }

    @Override
    public Inst clone(Map<Value, Value> replacementMap) {
        return new IAllocate(getType());
    }
}
