package frontend.symbol;

import frontend.symbol.datatype.DataType;

public class ValSymbol extends Symbol {
    public enum Type {
        Const, Var
    }

    private boolean isFuncParam = false;
    private DataType dataType;
    private Type type;

    public ValSymbol(String ident, Type type, DataType dataType) {
        super(ident);
        this.type = type;
        this.dataType = dataType;
    }

}
