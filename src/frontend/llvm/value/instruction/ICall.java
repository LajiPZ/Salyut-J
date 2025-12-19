package frontend.llvm.value.instruction;

import frontend.datatype.VoidType;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public Function getFunction() {
        return function;
    }

    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        if (function.getType() instanceof VoidType) {
            sb.append("call void ");
        } else {
            sb.append(getName()).append(" = call ").append(function.getType()).append(" ")    ;
        }
        sb.append("@").append(function.getName());
        sb.append("(");
        sb.append(
            getOperands().stream().map(Value::toString).collect(Collectors.joining(", "))
        );
        sb.append(")");
        return sb.toString();
    }

    @Override
    public Inst clone(Map<Value, Value> replacementMap) {
        List<Value> newArgs = new ArrayList<>();
        for (Value arg : getOperands()) {
            newArgs.add(replacementMap.getOrDefault(arg, arg));
        }
        return new ICall(
            getFunction(),
            newArgs
        );
    }


}
