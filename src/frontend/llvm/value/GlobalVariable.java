package frontend.llvm.value;

import frontend.datatype.DataType;
import frontend.datatype.init.ArrayInitType;
import frontend.datatype.init.ValInitType;
import frontend.symbol.ConstSymbol;
import frontend.symbol.ValSymbol;
import frontend.symbol.VarSymbol;

import java.util.Map;


public class GlobalVariable extends Value {
    private Value init;
    private Map<Integer, Value> initList;
    private ValSymbol symbol;

    public GlobalVariable(String name, DataType dataType, Value init) {
        super(name, dataType);
        this.init = init;
    }

    public GlobalVariable(String name, DataType dataType, Map<Integer, Value> initList) {
        super(name, dataType);
        this.initList = initList;
    }

    private static GlobalVariable create(ValSymbol symbol) {
        if (symbol.getInitType() instanceof ArrayInitType) {
            return new GlobalVariable(
                symbol.getIdent(), symbol.getDataType(),
                ((ArrayInitType)symbol.getInitType()).toValue()
            );
        } else {
            return new GlobalVariable(
                symbol.getIdent(), symbol.getDataType(),
                ((ValInitType)symbol.getInitType()).toValue()
            );
        }
    }

    public static GlobalVariable create(ConstSymbol symbol) {
        GlobalVariable result = create((ValSymbol) symbol);
        result.symbol = symbol;
        return result;
    }

    public static GlobalVariable create(VarSymbol symbol) {
        GlobalVariable result = create((ValSymbol) symbol);
        result.symbol = symbol;
        return result;
    }

}
