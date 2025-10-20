package frontend.symbol.datatype;

import java.util.ArrayList;

public class ArrayType extends DataType {
    public enum ElementType {
        Int
    }

    // 1. 把数组拍扁为一维 2. 记录每维度的index，以支持多维数组
    private ElementType elementType;
    private ArrayList<Integer> indexList;
    private ArrayList<DataType> elements;

    public ArrayType(ElementType elementType) {
        this.elementType = elementType;
        this.elements = new ArrayList<>();
        this.indexList = new ArrayList<>();
    }
}
