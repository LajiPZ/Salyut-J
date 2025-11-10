package frontend.llvm.tools;

import frontend.datatype.ArrayType;
import frontend.datatype.DataType;
import frontend.llvm.value.Value;

import java.util.Map;

public class ArrayInitStr {
    public static String getInitStr(DataType type, Map<Integer, Value> init) {
        StringBuilder sb = new StringBuilder();
        if (!(type instanceof ArrayType)) {

        }
        ArrayType arrayType = (ArrayType) type;
        DataType base = arrayType.getBaseType();
        sb.append(arrayType).append(" ");

        if (init == null) {
            sb.append("zeroinitializer");
        } else {
            boolean allZero = true;
            StringBuilder subBuilder = new StringBuilder();
            subBuilder.append("[");
            for (int i = 0; i < arrayType.getSize(); i++) {
                String res =
            }
            if (allZero) {
                sb.append("zeroinitializer");
            } else {
                sb.append(subBuilder.toString());
            }
        }

        return sb.toString();
    }
}
