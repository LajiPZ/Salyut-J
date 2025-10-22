package frontend.symbol.datatype.init;

import frontend.symbol.datatype.DataType;
import frontend.syntax.declaration.object.ConstInitVal;
import frontend.syntax.expression.Exp;

import java.util.*;

public class ArrayInitType extends InitType {

    // 1. 把数组拍扁为一维
    // 2. 记录每维度的index，以支持多维数组

    private List<Integer> indexList;
    private HashMap<Integer, Integer> elements;
    private DataType dataType;

    public ArrayInitType(List<Integer> indexList, DataType dataType) {
        this.indexList = indexList;
        this.dataType = dataType;
        this.elements = new HashMap<>();
    }

    public void set(int index, int value) {
        elements.put(index, value);
    }

    public static ArrayInitType createArrayInitType(List<Integer> indexList, ConstInitVal initVal, DataType baseType) {
        ArrayInitType array = new ArrayInitType(indexList, baseType);
        int index = 0;
        Queue<ConstInitVal> queue = new LinkedList<>();
        queue.add(initVal);
        while (!queue.isEmpty()) {
            ConstInitVal val = queue.poll();
            if (val.getSingleConstExp() != null) {
                array.set(index++, val.singleCalc());
            } else {
                queue.addAll(val.getSubInitVals());
            }
        }
        return array;
    }


    public int get(List<Exp> indexList) {
        assert indexList.size() == this.indexList.size();
        int index = indexList.get(indexList.size() - 1).calc();
        int currentLen = 1;
        for (int i = indexList.size() - 1; i > 0; i--) {
            currentLen *= this.indexList.get(i);
            index += indexList.get(i-1).calc() * currentLen;
        }
        return elements.getOrDefault(index, 0); // 此处为全局初始化值，所以给0
    }
}
