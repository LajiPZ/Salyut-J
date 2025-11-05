package frontend.symbol;

import frontend.symbol.datatype.DataType;
import frontend.symbol.datatype.init.InitType;

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
