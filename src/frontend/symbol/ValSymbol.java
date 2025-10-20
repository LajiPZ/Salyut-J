package frontend.symbol;

import frontend.symbol.datatype.init.InitDataType;

public class ValSymbol extends Symbol {
    public enum Type {
        Const, Var
    }

    private boolean isFuncParam = false;
    private InitDataType initDataType;
    private Type type;

    public ValSymbol(String ident, Type type, InitDataType initDataType) {
        super(ident);
        this.type = type;
        this.initDataType = initDataType;
    }

}
