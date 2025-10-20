package frontend.symbol;

import frontend.symbol.datatype.DataType;
import frontend.symbol.datatype.init.InitType;

public class VarSymbol extends ValSymbol{
    private boolean isStatic = false;

    public VarSymbol(String ident, boolean isStatic, DataType dataType) {
        super(ident, Type.Var, dataType);
        this.isStatic = isStatic;
    }

}
