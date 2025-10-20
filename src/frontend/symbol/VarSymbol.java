package frontend.symbol;

import frontend.symbol.datatype.DataType;

public class VarSymbol extends ValSymbol{
    boolean isStatic = false;

    public VarSymbol(String ident, boolean isStatic, DataType dataType) {
        super(ident, Type.Var, dataType);
        this.isStatic = isStatic;
    }

    public VarSymbol(String ident, DataType dataType) {
        super(ident, Type.Var, dataType);
        this.isStatic = false;
    }
}
