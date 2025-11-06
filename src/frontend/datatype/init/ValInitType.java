package frontend.datatype.init;

import frontend.datatype.DataType;

public class ValInitType extends InitType {
    private int value;
    private DataType dataType;

    public ValInitType(int value, DataType dataType) {
        this.value = value;
        this.dataType = dataType;
    }

    public int getValue() {
        return value;
    }

}
