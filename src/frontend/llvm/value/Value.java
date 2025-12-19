package frontend.llvm.value;

import frontend.datatype.DataType;
import utils.Counter;

public class Value {
    private String name;
    private final DataType type;
    public static Counter counter = new Counter(); // 用于生成局部value的编号，取一次加一个

    public Value(DataType type) {
        this("%" + counter.get(), type);
    }

    public Value(String name, DataType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }
    // TODO: uses

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return type.toString() + " " +  name;
    }
}
