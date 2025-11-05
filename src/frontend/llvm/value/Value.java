package frontend.llvm.value;

import frontend.symbol.datatype.DataType;
import utils.Counter;

public class Value {
    private String name;
    private final DataType type;
    public static Counter counter = new Counter(); // 用于生成局部value的编号，取一次加一个

    public Value(DataType type) {
        this("%" + Value.getCounter(), type);
    }

    public Value(String name, DataType type) {
        this.name = name;
        this.type = type;
    }


    // TODO: uses


}
