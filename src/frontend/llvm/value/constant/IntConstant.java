package frontend.llvm.value.constant;

import frontend.datatype.BaseType;
import frontend.datatype.BooleanType;
import frontend.datatype.DataType;
import frontend.datatype.IntType;
import frontend.llvm.value.Value;

public class IntConstant extends Value {
    private final int value;

    public IntConstant(int value) {
        super(String.valueOf(value), new IntType());
        this.value = value;
    }

    public IntConstant(int value, DataType type) {
        super(String.valueOf(value), type);
        this.value = value;
    }

    public final static IntConstant zero = new IntConstant(0);
    public final static IntConstant logicZero = new IntConstant(0, new BooleanType());
    public final static IntConstant logicOne = new IntConstant(1, new BooleanType());
}
