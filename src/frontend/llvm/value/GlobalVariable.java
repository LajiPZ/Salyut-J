package frontend.llvm.value;

import frontend.datatype.DataType;
import frontend.datatype.init.ArrayInitType;
import frontend.datatype.init.ValInitType;
import frontend.symbol.ConstSymbol;
import frontend.symbol.ValSymbol;
import frontend.symbol.VarSymbol;

import java.util.Map;

public class GlobalVariable extends Value {
    private enum Type {
        Single, Multi
    }

    private Value init;
    private Map<Integer, Value> initList;
    private ValSymbol symbol;
    private Type type;


    public GlobalVariable(String name, DataType dataType, Value init) {
        // Init = IntConst..
        super(name, dataType);
        this.init = init;
        this.type = Type.Single;
    }

    public GlobalVariable(String name, DataType dataType, Map<Integer, Value> initList) {
        // Init = null / Map<Int, IntConst>...
        super(name, dataType);
        this.initList = initList;
        this.type = Type.Multi;
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

    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(getName()).append(" = dso_local global ");
        if (type == Type.Single) {
            sb.append(init);
        }
    }
}
