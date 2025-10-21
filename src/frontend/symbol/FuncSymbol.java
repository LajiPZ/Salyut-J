package frontend.symbol;

import frontend.symbol.datatype.DataType;

import java.util.ArrayList;
import java.util.List;

public class FuncSymbol extends Symbol {

    private DataType type;
    private ArrayList<ValSymbol> parameters;

    public FuncSymbol(String ident, DataType type) {
        super(ident);
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
}
