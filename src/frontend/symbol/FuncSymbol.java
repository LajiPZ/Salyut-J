package frontend.symbol;

import frontend.symbol.datatype.DataType;
import frontend.symbol.datatype.IntType;
import frontend.symbol.datatype.VoidType;

import java.util.ArrayList;
import java.util.List;

public class FuncSymbol extends Symbol {

    private DataType type;
    private ArrayList<ValSymbol> parameters;

    public FuncSymbol(String ident, DataType type, int scopeCnt) {
        super(ident, scopeCnt);
        this.type = type;
        this.parameters = new ArrayList<>();
    }

    public void addParameter(ValSymbol val) {
        this.parameters.add(val);
    }

    public int getParameterCount() {
        return parameters.size();
    }

    public List<ValSymbol> getParameters() {
        return parameters;
    }

    public DataType getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (type instanceof VoidType) sb.append("Void");
        else if (type instanceof IntType) sb.append("Int");
        sb.append("Func");
        return sb.toString();
    }
}
