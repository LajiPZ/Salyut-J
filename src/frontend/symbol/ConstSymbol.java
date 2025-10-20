package frontend.symbol;

import frontend.symbol.datatype.DataType;
import frontend.symbol.datatype.init.InitType;

public class ConstSymbol extends ValSymbol {
    public ConstSymbol(String ident, DataType dataType) {
        super(ident, Type.Const, dataType);
    }
}
