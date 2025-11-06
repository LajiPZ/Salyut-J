package frontend.llvm.value;

import frontend.datatype.DataType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Function extends Value {
    private final List<BBlock> bBlocks;
    private final List<Value> params;
    private boolean isExtern = false;

    // TODO: valCounter

    public Function(String name, DataType type) {
        super(name, type);
        this.bBlocks = new ArrayList<>();
        this.params = new ArrayList<>();
    }

    public Value addParam(DataType type) {
        Value param = new Value(type);
        params.add(param);
        return param;
    }

    public static Function extern(String name, DataType type, DataType... args) {
        Function func = new Function(name, type);
        func.isExtern = true;
        for (DataType arg : args) {
            // declare只需输出类型，Value.counter的事情再议
            func.addParam(arg);
        }
        // 为了保证第一个函数定义，从参数开始，以%0开始计数，重置一下计数器
        Value.counter.reset();
        return func;
    }

    public void addBBlock(BBlock bBlock) {
        bBlocks.add(bBlock);
    }



    @Override
    public String toString() {
        // 输出函数定义，external函数此处不关心
        return "define dso_local " +
                getType() +
                " @" + getName() +
                "(" +
                params.stream().map(Value::toString).collect(Collectors.joining(", ")) +
                ") {\n" +
                bBlocks.stream().map(Value::toString).collect(Collectors.joining("\n")) +
                "}\n";
    }
}
