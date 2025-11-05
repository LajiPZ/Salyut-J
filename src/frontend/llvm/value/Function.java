package frontend.llvm.value;

import frontend.symbol.datatype.DataType;

import java.util.ArrayList;
import java.util.List;

public class Function extends Value {
    private final List<BBlock> bBlocks;
    private final List<Value> params;
    private boolean isExtern = false;

    private int counter;

    public Function(String name, DataType type) {
        super(name, type);
        this.bBlocks = new ArrayList<>();
        this.params = new ArrayList<>();
        this.counter = 0;
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

}
