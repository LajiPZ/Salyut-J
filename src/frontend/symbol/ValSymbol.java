package frontend.symbol;

import frontend.symbol.datatype.DataType;
import frontend.symbol.datatype.init.InitType;

public class ValSymbol extends Symbol {
    public enum Type {
        Const, Var
    }

    private boolean isFuncParam = false;
    private InitType initType = null;
    private Type type;
    private DataType dataType;

    public ValSymbol(String ident, Type type, DataType dataType) {
        super(ident);
        this.type = type;
        this.dataType = dataType;
    }

    public void setInitType(InitType initType) {
        this.initType = initType;
    }

    public boolean hasInitType() {
        return initType != null;
    }

    public InitType getInitType() {
        return initType;
    }

    public DataType getDataType() {
        return dataType;
    }
}
