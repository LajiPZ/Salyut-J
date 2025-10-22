package frontend.symbol.datatype;

import frontend.syntax.expression.ConstExp;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ArrayType extends DataType{

    private DataType baseType;
    private int size;

    public ArrayType(DataType baseType, int size) {
        this.baseType = baseType;
        this.size = size;
    }

    /**
     * 若!indexExp.isEmpty()，返回数组，没有则返回baseType
     * @return
     */
    public static DataType createDataType(DataType baseType, List<ConstExp> indexExps) {
        DataType base = baseType;
        for (int i = indexExps.size() - 1; i >= 0; i--) {
            if (indexExps.get(i) == null) {
                base = new PointerType(baseType);
            } else {
                base = new ArrayType(base, indexExps.get(i).calc());
            }
        }
        return base;
    }

    public List<Integer> getIndexList() {
        if (baseType instanceof ArrayType) {
            LinkedList<Integer> list = new LinkedList<>();
            list.add(size);
            list.addAll(((ArrayType)baseType).getIndexList());
            return list;
        } else {
            return Collections.singletonList(size);
        }
    }

    public DataType getBaseType() {
        return baseType;
    }


    @Override
    public boolean compatibleWith(DataType other) {
        return other instanceof ArrayType && ((ArrayType)other).getBaseType().compatibleWith(baseType);
    }

}
