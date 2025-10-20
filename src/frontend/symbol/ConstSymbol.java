package frontend.symbol;

import frontend.symbol.datatype.init.InitDataType;

public class ConstSymbol extends ValSymbol {
    public ConstSymbol(String ident, InitDataType initDataType) {
        super(ident, Type.Const, initDataType);
    }
}
