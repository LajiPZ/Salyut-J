package frontend.llvm.value.instruction;

import frontend.datatype.VoidType;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;

import java.util.List;

public class ICall extends Inst {
    private final Function function;

    public ICall(Function function, List<Value> args) {
        super(
            function.getType() instanceof VoidType ? "" : ("%" + Value.counter.get()), function.getType()
        );
        this.function = function;
        for (Value arg : args) {
            addOperand(arg);
        }
    }

    @Override
    public String toLLVM() {
        // TODO
        return "";
    }
}
