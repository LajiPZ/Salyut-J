package frontend.datatype.init;

import frontend.datatype.DataType;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;

public class ValInitType extends InitType {
    private int value;
    private DataType dataType;

    public ValInitType(int value, DataType dataType) {
        this.value = value;
        this.dataType = dataType;
    }

    public int getValue() {
        return value;
    }

    public Value toValue() {
        return new IntConstant(value, dataType);
    }
}
