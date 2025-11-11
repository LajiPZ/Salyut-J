package frontend.llvm.tools;

import frontend.datatype.ArrayType;
import frontend.datatype.DataType;
import frontend.llvm.value.Value;
import utils.Pair;

import java.util.Map;

public class ArrayInitStr {
    public static String getInitStr(DataType type, Map<Integer, Value> init) {
        return getInitStr(type,init,0).getValue1();
    }

    private static Pair<String, Boolean> getInitStr(DataType type, Map<Integer, Value> init, int idx) {
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
            for (int i = 0; i < arrayType.getLength(); i++) {
                if (i != 0) {
                    subBuilder.append(", ");
                }
                Pair<String, Boolean> res = getInitStr(base, init, idx + i * base.getSize() / type.getFinalDataType().getSize());
                subBuilder.append(res.getValue1());
                allZero &= res.getValue2();
            }
            subBuilder.append("]");
            if (allZero) {
                sb.append("zeroinitializer");
            } else {
                sb.append(subBuilder.toString());
            }
        }

        return sb.toString();
    }
}
