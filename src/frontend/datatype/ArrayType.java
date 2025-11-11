package frontend.datatype;

import frontend.syntax.expression.ConstExp;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ArrayType extends DataType {

    private DataType baseType;
    private int length;

    public ArrayType(DataType baseType, int size) {
        this.baseType = baseType;
        this.length = size;
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
            list.add(length);
            list.addAll(((ArrayType)baseType).getIndexList());
            return list;
        } else {
            return Collections.singletonList(length);
        }
    }

    public DataType getBaseType() {
        return baseType;
    }

    public int getLength() {
        return length;
    }

    public int getSize() {
        return length * baseType.getSize();
    }

    @Override
    public boolean compatibleWith(DataType other) {
        return other instanceof ArrayType && ((ArrayType)other).getBaseType().compatibleWith(baseType);
    }

    @Override
    public String toString() {
        return "[" + length + " x " + baseType.toString() + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ArrayType && ((ArrayType) obj).getBaseType().equals(baseType) && ((ArrayType) obj).length == length;
    }
}
