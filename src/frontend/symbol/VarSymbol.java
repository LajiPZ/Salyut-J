package frontend.symbol;

import frontend.symbol.datatype.init.InitDataType;

public class VarSymbol extends ValSymbol{
    boolean isStatic = false;

    public VarSymbol(String ident, boolean isStatic, InitDataType initDataType) {
        super(ident, Type.Var, initDataType);
        this.isStatic = isStatic;
    }

    public VarSymbol(String ident, InitDataType initDataType) {
        super(ident, Type.Var, initDataType);
        this.isStatic = false;
    }
}
