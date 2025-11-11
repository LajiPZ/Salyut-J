package frontend.llvm.tools;

import frontend.datatype.BooleanType;
import frontend.datatype.DataType;
import frontend.datatype.IntType;
import frontend.datatype.PointerType;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.ICompare;
import frontend.llvm.value.instruction.IConvert;
import frontend.llvm.value.instruction.Inst;
import frontend.llvm.value.instruction.Operator;

import java.util.function.Function;

final public class ValueConverter {
    private static Function<Inst, Value> builderComm;

    public static void setBuilderComm(Function<Inst, Value> builderComm) {
        ValueConverter.builderComm = builderComm;
    }

    public static Value to(DataType target, Value val) {
        if (val.getType().equals(target)) {
            return val;
        } else {
            return builderComm.apply(new IConvert(target, val));
        }
    }

    public static Value toInteger(Value val) {
        return to(new IntType(), val);
    }

    public static Value toBoolean(Value val) {
        if (val.getType() instanceof BooleanType) {
            return val;
        } else {
            return builderComm.apply(new ICompare(
                Operator.NE,
                val,
                new IntConstant(0, val.getType())
            ));
        }
    }

    public static Value toPtrBaseType(Value pointer, Value val) {
        assert pointer.getType() instanceof PointerType : "You should use a pointer";
        return to(
            ((PointerType)(pointer.getType())).getBaseType(),
            val
        );
    }
}
