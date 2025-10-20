package frontend.symbol.datatype.init;

import frontend.symbol.datatype.DataType;
import frontend.syntax.declaration.object.ConstInitVal;
import frontend.syntax.expression.Exp;

import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.List;

public class ArrayInitType extends InitType {

    // 1. 把数组拍扁为一维 2. 记录每维度的index，以支持多维数组

    private List<Integer> indexList;
    private ArrayList<Integer> elements;
    private DataType dataType;

    public ArrayInitType(List<Integer> indexList, DataType dataType) {
        this.indexList = indexList;
        this.dataType = dataType;
        this.elements = new ArrayList<>();
    }

    public static ArrayInitType createArrayInitType(List<Integer> indexList, ConstInitVal initVal, DataType baseType) {

        return new ArrayInitType(indexList, );
    }


    public int get(List<Exp> indexList) {
        assert indexList.size() == this.indexList.size();

    }
}
