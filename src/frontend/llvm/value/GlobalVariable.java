package frontend.llvm.value;

import frontend.datatype.ArrayType;
import frontend.datatype.DataType;
import frontend.datatype.init.ArrayInitType;
import frontend.datatype.init.ValInitType;
import frontend.llvm.tools.ArrayInitStr;
import frontend.llvm.value.constant.IntConstant;
import frontend.symbol.ConstSymbol;
import frontend.symbol.Symbol;
import frontend.symbol.ValSymbol;
import frontend.symbol.VarSymbol;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    public ValSymbol getSymbol() {
        return symbol;
    }

    private static GlobalVariable create(ValSymbol symbol) {
        return create(symbol, symbol.getIdent());
    }

    private static GlobalVariable create(ValSymbol symbol, String ident) {
        if (symbol.getInitType() instanceof ArrayInitType) {
            return new GlobalVariable(
                ident, symbol.getDataType(),
                ((ArrayInitType)symbol.getInitType()).toValue()
            );
        } else {
            return new GlobalVariable(
                ident, symbol.getDataType(),
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

    public static GlobalVariable create(VarSymbol symbol, String ident) {
        GlobalVariable result = create((ValSymbol) symbol, ident);
        result.symbol = symbol;
        return result;
    }

    public List<Integer> getInitList() {
        if (type == Type.Single) {
            return List.of(((IntConstant) init).getValue());
        } else {
            return initMaptoList(initList);
        }
    }

    public List<Integer> initMaptoList(Map<Integer, Value> initList) {
        return IntStream.range(0, ((ArrayType) getType()).getLength())
            .map(i -> initList == null ? 0 : ((IntConstant) initList.getOrDefault(i, new IntConstant(0))).getValue())
            .boxed().collect(Collectors.toList());
    }

    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(getName()).append(" = dso_local global ");
        if (type == Type.Single) {
            sb.append(init);
        } else {
            sb.append(ArrayInitStr.getInitStr(getType(), initList));
        }
        return sb.toString();
    }
}
