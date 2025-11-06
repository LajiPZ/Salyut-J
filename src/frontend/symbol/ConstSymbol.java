package frontend.symbol;

import frontend.datatype.DataType;

public class ConstSymbol extends ValSymbol {
    public ConstSymbol(String ident, DataType dataType, int scopeCnt) {
        super(ident, Type.Const, dataType, scopeCnt);
    }

}
