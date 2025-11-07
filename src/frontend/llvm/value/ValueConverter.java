package frontend.llvm.value;

import frontend.datatype.BooleanType;
import frontend.datatype.DataType;
import frontend.datatype.IntType;
import frontend.datatype.PointerType;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.ICompare;
import frontend.llvm.value.instruction.IConvert;
import frontend.llvm.value.instruction.Operator;

final public class ValueConverter {
    public static Value to(DataType target, Value val) {
        if (val.getType().equals(target)) {
            return val;
        } else {
            return new IConvert(target, val);
        }
    }

    public static Value toBoolean(Value val) {
        if (val.getType() instanceof BooleanType) {
            return val;
        } else {
            return new ICompare(
                Operator.NE,
                val,
                new IntConstant(0, val.getType())
            );
        }
    }

    public static Value toBaseType(Value pointer, Value val) {
        assert pointer.getType() instanceof PointerType : "You should use a pointer";
        return to(
            ((PointerType)(pointer.getType())).getBaseType(),
            val
        );
    }
}
