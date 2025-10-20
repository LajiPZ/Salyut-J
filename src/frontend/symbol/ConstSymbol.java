package frontend.symbol;

import frontend.symbol.datatype.DataType;

public class ConstSymbol extends ValSymbol {
    public ConstSymbol(String ident, DataType dataType) {
        super(ident, Type.Const, dataType);
    }
}
