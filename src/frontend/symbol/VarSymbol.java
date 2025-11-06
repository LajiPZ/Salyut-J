package frontend.symbol;

import frontend.datatype.DataType;

public class VarSymbol extends ValSymbol {
    private boolean isStatic = false;

    public VarSymbol(String ident, boolean isStatic, DataType dataType, int scopeCnt) {
        super(ident, Type.Var, dataType, scopeCnt);
        this.isStatic = isStatic;
    }

    public boolean isStatic() {
        return isStatic;
    }
}
